package com.cortex.app.domain.graph

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Deterministic entity extraction (MemoryEngine.md Step 4 / Phase 2 Step 3). No AI calls
 * — every extraction here is regex, dictionary, or rule based, per the Cortex
 * constitution's "algorithms before AI."
 *
 * Candidates from every pattern are gathered independently, then resolved with greedy
 * interval scheduling: sort by confidence (specific patterns are seeded with higher
 * confidence than generic ones), then by span length, then by position, and accept each
 * candidate only if it doesn't overlap an already-accepted span. This means a
 * high-confidence match (a backtick-quoted command, an ISO date) always wins over a
 * low-confidence one (a bare number) that happens to overlap it, without needing a
 * hand-maintained pass ordering.
 */
class EntityExtractor {

    private val techTrie = EntityDictionaries.buildTechDictionaryTrie()

    fun extract(text: String, referenceTime: Instant = Instant.now()): List<ExtractedEntity> {
        if (text.isBlank()) return emptyList()

        val pointCandidates = mutableListOf<ExtractedEntity>()
        pointCandidates += extractUrlsAndRepos(text)
        pointCandidates += extractBackTickCommands(text)
        pointCandidates += extractCliCommandLines(text)
        pointCandidates += extractResearchPapers(text)
        pointCandidates += extractGitCommits(text)
        pointCandidates += extractBugIds(text)
        pointCandidates += extractPackageNames(text)
        pointCandidates += extractBranchNames(text)
        pointCandidates += extractVersions(text)
        pointCandidates += extractFilePaths(text)
        pointCandidates += extractDates(text, referenceTime)
        pointCandidates += extractTimes(text)
        pointCandidates += extractPersonMentions(text)
        pointCandidates += extractProjectMentions(text)
        pointCandidates += extractTechDictionaryTerms(text)
        pointCandidates += extractNumbers(text)

        // TASK / GOAL / ERROR_MESSAGE are phrase-level tags over a whole clause or line,
        // not point entities — a GOAL span legitimately contains a DATE inside it, an
        // ERROR_MESSAGE legitimately contains a BUG_ID. Resolving them in the same
        // overlap competition as point entities meant a long structural span would lose
        // outright to a short, higher-confidence entity nested inside it. They get their
        // own resolution pass instead, so both layers survive.
        val structuralCandidates = mutableListOf<ExtractedEntity>()
        structuralCandidates += extractErrorMessages(text)
        structuralCandidates += extractTaskLines(text)
        structuralCandidates += extractGoalLines(text)

        val resolvedPoints = resolveOverlaps(pointCandidates)
        val resolvedStructural = resolveOverlaps(structuralCandidates)

        return (resolvedPoints + resolvedStructural).sortedBy { it.startIndex }
    }

    // --- overlap resolution -------------------------------------------------------

    private fun resolveOverlaps(candidates: List<ExtractedEntity>): List<ExtractedEntity> {
        val sorted = candidates.sortedWith(
            compareByDescending<ExtractedEntity> { it.confidence }
                .thenByDescending { it.endIndex - it.startIndex }
                .thenBy { it.startIndex }
        )
        val accepted = mutableListOf<ExtractedEntity>()
        val claimed = mutableListOf<IntRange>()
        for (candidate in sorted) {
            val range = candidate.startIndex until candidate.endIndex
            val overlaps = claimed.any { it.first < range.last + 1 && range.first < it.last + 1 }
            if (!overlaps) {
                claimed.add(range)
                accepted.add(candidate)
            }
        }
        return accepted.sortedBy { it.startIndex }
    }

    // --- URL / repository -----------------------------------------------------------

    private val urlRegex = Regex("""\bhttps?://[^\s<>"')]+""")
    private val repoHosts = listOf("github.com", "gitlab.com", "bitbucket.org")

    private fun extractUrlsAndRepos(text: String): List<ExtractedEntity> =
        urlRegex.findAll(text).map { m ->
            val trimmed = m.value.trimEnd('.', ',', ')', ';', ':')
            val isRepo = repoHosts.any { host -> trimmed.contains(host, ignoreCase = true) }
            ExtractedEntity(
                kind = if (isRepo) EntityKind.REPOSITORY else EntityKind.URL,
                rawValue = trimmed,
                normalizedValue = trimmed.lowercase().trimEnd('/'),
                startIndex = m.range.first,
                endIndex = m.range.first + trimmed.length,
                confidence = 0.95
            )
        }.toList()

    // --- commands ---------------------------------------------------------------

    private val backtickRegex = Regex("`([^`\n]+)`")
    private val cliPrefixes = listOf(
        "$ ", "sudo ", "git ", "adb ", "./gradlew", "gradlew ", "kotlinc ", "npm ",
        "pip ", "pip3 ", "curl ", "docker ", "kubectl ", "python ", "python3 "
    )

    private fun extractBackTickCommands(text: String): List<ExtractedEntity> =
        backtickRegex.findAll(text).mapNotNull { m ->
            val group = m.groups[1] ?: return@mapNotNull null
            ExtractedEntity(
                kind = EntityKind.COMMAND,
                rawValue = group.value,
                normalizedValue = group.value.trim(),
                startIndex = group.range.first,
                endIndex = group.range.last + 1,
                confidence = 0.9
            )
        }.toList()

    private fun extractCliCommandLines(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        var offset = 0
        for (line in text.split("\n")) {
            val trimmedStart = line.trimStart()
            val leadingWhitespace = line.length - trimmedStart.length
            if (cliPrefixes.any { trimmedStart.startsWith(it, ignoreCase = true) }) {
                val value = trimmedStart.trimEnd()
                if (value.isNotBlank()) {
                    val start = offset + leadingWhitespace
                    results.add(
                        ExtractedEntity(
                            kind = EntityKind.COMMAND,
                            rawValue = value,
                            normalizedValue = value,
                            startIndex = start,
                            endIndex = start + value.length,
                            confidence = 0.6
                        )
                    )
                }
            }
            offset += line.length + 1 // account for the split "\n"
        }
        return results
    }

    // --- research papers ----------------------------------------------------------

    private val arxivRegex = Regex("""\barxiv[:\s]?(\d{4}\.\d{4,5})\b""", RegexOption.IGNORE_CASE)
    private val doiRegex = Regex("""\bdoi:\s?10\.\d{4,9}/\S+\b""", RegexOption.IGNORE_CASE)
    private val etAlRegex = Regex("""\b[A-Z][a-zA-Z\-]+\set al\.?(?:\s*\(\d{4}\))?""")

    private fun extractResearchPapers(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        arxivRegex.findAll(text).forEach { m ->
            results.add(
                ExtractedEntity(
                    EntityKind.RESEARCH_PAPER, m.value, "arxiv:${m.groupValues[1]}",
                    m.range.first, m.range.last + 1, 0.9
                )
            )
        }
        doiRegex.findAll(text).forEach { m ->
            results.add(
                ExtractedEntity(
                    EntityKind.RESEARCH_PAPER, m.value, m.value.lowercase(),
                    m.range.first, m.range.last + 1, 0.9
                )
            )
        }
        etAlRegex.findAll(text).forEach { m ->
            results.add(
                ExtractedEntity(
                    EntityKind.RESEARCH_PAPER, m.value, m.value.trim(),
                    m.range.first, m.range.last + 1, 0.65
                )
            )
        }
        return results
    }

    // --- git commits / bug ids / package names / branch names / versions ----------

    private val gitCommitRegex = Regex("""\b[0-9a-f]{7,40}\b""", RegexOption.IGNORE_CASE)
    private val commitContextRegex = Regex("""(?:commit|sha|rev(?:ision)?)\s*:?\s*$""", RegexOption.IGNORE_CASE)

    private fun extractGitCommits(text: String): List<ExtractedEntity> =
        gitCommitRegex.findAll(text).mapNotNull { m ->
            val hasLetter = m.value.any { it in 'a'..'f' || it in 'A'..'F' }
            val precedingContext = text.substring(maxOf(0, m.range.first - 12), m.range.first)
            val hasContext = commitContextRegex.containsMatchIn(precedingContext)
            if (!hasLetter && !hasContext) return@mapNotNull null
            ExtractedEntity(
                EntityKind.GIT_COMMIT, m.value, m.value.lowercase(),
                m.range.first, m.range.last + 1,
                if (hasContext) 0.9 else 0.55
            )
        }.toList()

    private val bugIdRegex = Regex("""\b(?:#\d{2,6}|[A-Z]{2,10}-\d{1,6})\b""")

    private fun extractBugIds(text: String): List<ExtractedEntity> =
        bugIdRegex.findAll(text).map { m ->
            ExtractedEntity(
                EntityKind.BUG_ID, m.value, m.value.uppercase(),
                m.range.first, m.range.last + 1, 0.85
            )
        }.toList()

    private val packageNameRegex = Regex("""\b(?:[a-z][a-z0-9]*\.){2,}[a-z][a-z0-9]*\b""")

    private fun extractPackageNames(text: String): List<ExtractedEntity> =
        packageNameRegex.findAll(text).map { m ->
            ExtractedEntity(
                EntityKind.PACKAGE_NAME, m.value, m.value.lowercase(),
                m.range.first, m.range.last + 1, 0.7
            )
        }.toList()

    private val branchNameRegex =
        Regex("""\b(?:feature|fix|bugfix|hotfix|release|chore|refactor)/[\w\-.]+\b""", RegexOption.IGNORE_CASE)

    private fun extractBranchNames(text: String): List<ExtractedEntity> =
        branchNameRegex.findAll(text).map { m ->
            ExtractedEntity(
                EntityKind.BRANCH_NAME, m.value, m.value.lowercase(),
                m.range.first, m.range.last + 1, 0.75
            )
        }.toList()

    private val versionRegex = Regex("""\bv?\d+\.\d+(?:\.\d+)?(?:-[a-zA-Z0-9.]+)?\b""")

    private fun extractVersions(text: String): List<ExtractedEntity> =
        versionRegex.findAll(text).map { m ->
            ExtractedEntity(
                EntityKind.VERSION, m.value, m.value.removePrefix("v"),
                m.range.first, m.range.last + 1, 0.75
            )
        }.toList()

    // --- file paths -----------------------------------------------------------------

    private val filePathRegex =
        Regex("""(?<![\w])(?:\.{1,2}/[\w.\-]+(?:/[\w.\-]+)*|/(?:[\w.\-]+/)+[\w.\-]+)""")

    private fun extractFilePaths(text: String): List<ExtractedEntity> =
        filePathRegex.findAll(text).map { m ->
            val trimmed = m.value.trimEnd('.')
            ExtractedEntity(
                EntityKind.FILE_PATH, trimmed, trimmed,
                m.range.first, m.range.first + trimmed.length, 0.65
            )
        }.toList()

    // --- error messages (extracted as a signal; not promoted to graph nodes) -------

    private val errorMessageRegex =
        Regex("""(?:[A-Za-z.]*Exception|[A-Za-z]+Error|Traceback[^\n]*|panic:[^\n]*|fatal:[^\n]*)[^\n]{0,80}""")

    private fun extractErrorMessages(text: String): List<ExtractedEntity> =
        errorMessageRegex.findAll(text).map { m ->
            ExtractedEntity(
                EntityKind.ERROR_MESSAGE, m.value, m.value.trim(),
                m.range.first, m.range.last + 1, 0.6
            )
        }.toList()

    // --- dates / times ------------------------------------------------------------

    private val isoDateRegex = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
    private val slashDateRegex = Regex("""\b\d{1,2}/\d{1,2}/\d{2,4}\b""")
    private val monthNameRegex = Regex(
        """\b(January|February|March|April|May|June|July|August|September|October|November|December)\.?\s+(\d{1,2})(?:st|nd|rd|th)?(?:,?\s+(\d{4}))?\b""",
        RegexOption.IGNORE_CASE
    )
    private val relativeDateRegex = Regex("""\b(today|tomorrow|yesterday)\b""", RegexOption.IGNORE_CASE)
    private val weekdayRegex = Regex(
        """\b(next|this)\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""",
        RegexOption.IGNORE_CASE
    )

    private fun extractDates(text: String, referenceTime: Instant): List<ExtractedEntity> {
        val zone = ZoneId.systemDefault()
        val today = referenceTime.atZone(zone).toLocalDate()
        val results = mutableListOf<ExtractedEntity>()

        isoDateRegex.findAll(text).forEach { m ->
            results.add(ExtractedEntity(EntityKind.DATE, m.value, m.value, m.range.first, m.range.last + 1, 0.9))
        }
        slashDateRegex.findAll(text).forEach { m ->
            val parsed = runCatching {
                val parts = m.value.split("/").map { it.toInt() }
                val year = if (parts[2] < 100) 2000 + parts[2] else parts[2]
                LocalDate.of(year, parts[0], parts[1])
            }.getOrNull()
            results.add(
                ExtractedEntity(
                    EntityKind.DATE, m.value, parsed?.toString() ?: m.value,
                    m.range.first, m.range.last + 1, 0.7
                )
            )
        }
        monthNameRegex.findAll(text).forEach { m ->
            val month = runCatching {
                Month.entries.first { it.getDisplayName(TextStyle.FULL, Locale.ENGLISH).equals(m.groupValues[1], true) }
            }.getOrNull()
            val day = m.groupValues[2].toIntOrNull()
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            val parsed = if (month != null && day != null) runCatching { LocalDate.of(year, month, day) }.getOrNull() else null
            results.add(
                ExtractedEntity(
                    EntityKind.DATE, m.value, parsed?.toString() ?: m.value,
                    m.range.first, m.range.last + 1, 0.8
                )
            )
        }
        relativeDateRegex.findAll(text).forEach { m ->
            val resolved = when (m.value.lowercase()) {
                "today" -> today
                "tomorrow" -> today.plusDays(1)
                else -> today.minusDays(1)
            }
            results.add(
                ExtractedEntity(EntityKind.DATE, m.value, resolved.toString(), m.range.first, m.range.last + 1, 0.85)
            )
        }
        weekdayRegex.findAll(text).forEach { m ->
            val targetDow = DayOfWeek.valueOf(m.groupValues[2].uppercase())
            var candidate = today.plusDays(1)
            while (candidate.dayOfWeek != targetDow) candidate = candidate.plusDays(1)
            if (m.groupValues[1].equals("this", true) && candidate.minusDays(7) >= today) {
                candidate = candidate.minusDays(7)
            }
            results.add(
                ExtractedEntity(EntityKind.DATE, m.value, candidate.toString(), m.range.first, m.range.last + 1, 0.7)
            )
        }
        return results
    }

    private val timeRegex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)(?:\s?([APap][Mm]))?\b""")
    private val bareAmPmRegex = Regex("""\b(\d{1,2})\s?([APap][Mm])\b""")

    private fun extractTimes(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        timeRegex.findAll(text).forEach { m ->
            results.add(ExtractedEntity(EntityKind.TIME, m.value, m.value.lowercase(), m.range.first, m.range.last + 1, 0.8))
        }
        bareAmPmRegex.findAll(text).forEach { m ->
            results.add(ExtractedEntity(EntityKind.TIME, m.value, m.value.lowercase(), m.range.first, m.range.last + 1, 0.55))
        }
        return results
    }

    // --- people / projects / tasks / goals (rule-based, conservative) -------------

    private val handleRegex = Regex("""(?<!\w)@[A-Za-z0-9_]{2,30}\b""")
    private val personCueRegex = Regex(
        """\b(?:with|from|by|told|asked|met|call|email|cc|assigned to)\s+([A-Z][a-z]+\s[A-Z][a-z]+)\b"""
    )

    private fun extractPersonMentions(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        handleRegex.findAll(text).forEach { m ->
            results.add(ExtractedEntity(EntityKind.PERSON, m.value, m.value.lowercase(), m.range.first, m.range.last + 1, 0.85))
        }
        personCueRegex.findAll(text).forEach { m ->
            val g = m.groups[1] ?: return@forEach
            results.add(ExtractedEntity(EntityKind.PERSON, g.value, g.value, g.range.first, g.range.last + 1, 0.55))
        }
        return results
    }

    private val projectPatternA = Regex("""\bthe\s+([A-Z][\w]*)\s+project\b""")
    private val projectPatternB = Regex("""\bproject\s+([A-Z][\w]*)\b""")
    private val projectPatternC = Regex("""\b([A-Z][\w]*)\s+project\b""")

    private fun extractProjectMentions(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        for (regex in listOf(projectPatternA, projectPatternB, projectPatternC)) {
            regex.findAll(text).forEach { m ->
                val g = m.groups[1] ?: return@forEach
                results.add(ExtractedEntity(EntityKind.PROJECT, g.value, g.value, g.range.first, g.range.last + 1, 0.58))
            }
        }
        return results
    }

    private val taskLineRegex = Regex("""(?m)^\s*(?:Task:|TODO:|-\s?\[\s?\])\s*(.{1,120})$""", RegexOption.IGNORE_CASE)

    private fun extractTaskLines(text: String): List<ExtractedEntity> =
        taskLineRegex.findAll(text).mapNotNull { m ->
            val g = m.groups[1] ?: return@mapNotNull null
            val value = g.value.trim()
            if (value.isEmpty()) return@mapNotNull null
            ExtractedEntity(EntityKind.TASK, value, value, g.range.first, g.range.last + 1, 0.75)
        }.toList()

    private val goalLineRegex = Regex("""(?m)^\s*(?:Goal:|Objective:)\s*(.{1,120})$""", RegexOption.IGNORE_CASE)
    private val goalInlineRegex = Regex("""\b(?:goal is to|aiming to|objective is to)\s+([^.\n]{3,80})""", RegexOption.IGNORE_CASE)

    private fun extractGoalLines(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        goalLineRegex.findAll(text).forEach { m ->
            val g = m.groups[1] ?: return@forEach
            val value = g.value.trim()
            if (value.isNotEmpty()) {
                results.add(ExtractedEntity(EntityKind.GOAL, value, value, g.range.first, g.range.last + 1, 0.75))
            }
        }
        goalInlineRegex.findAll(text).forEach { m ->
            val g = m.groups[1] ?: return@forEach
            val value = g.value.trim()
            if (value.isNotEmpty()) {
                results.add(ExtractedEntity(EntityKind.GOAL, value, value, g.range.first, g.range.last + 1, 0.6))
            }
        }
        return results
    }

    // --- dictionary-based tech terms (Trie) ----------------------------------------

    private fun extractTechDictionaryTerms(text: String): List<ExtractedEntity> {
        data class Tok(val text: String, val start: Int, val end: Int)

        val tokenRegex = Regex("[\\p{L}0-9+#.]+")
        val tokens = tokenRegex.findAll(text).mapNotNull { m ->
            val trimmed = m.value.trim('.')
            if (trimmed.isEmpty()) return@mapNotNull null
            val leadingDots = m.value.length - m.value.trimStart('.').length
            Tok(trimmed, m.range.first + leadingDots, m.range.first + leadingDots + trimmed.length)
        }.toList()
        if (tokens.isEmpty()) return emptyList()

        val matches = techTrie.findAll(tokens.map { it.text })
        return matches.map { (startTokenIdx, lengthTokens, kind) ->
            val first = tokens[startTokenIdx]
            val last = tokens[startTokenIdx + lengthTokens - 1]
            val raw = text.substring(first.start, last.end)
            ExtractedEntity(kind, raw, raw.lowercase(), first.start, last.end, 0.8)
        }
    }

    // --- numbers (lowest-priority catch-all) ---------------------------------------

    private val numberRegex = Regex("""\b\d+(?:\.\d+)?\b""")

    private fun extractNumbers(text: String): List<ExtractedEntity> =
        numberRegex.findAll(text).map { m ->
            ExtractedEntity(EntityKind.NUMBER, m.value, m.value, m.range.first, m.range.last + 1, 0.4)
        }.toList()
}
