# Cortex Guidance Engine

## Purpose
Help the user think, plan, and follow through without becoming noisy or intrusive.

## Guidance modes
### Passive
Only respond when asked.

### Proactive
Surface useful insights, reminders, or patterns when they are likely to help.

### Thinking mode
Ask clarifying questions that reduce ambiguity and improve reasoning.

### Daily brief
Summarize:
- important recent thoughts
- unfinished tasks
- neglected projects
- recurring blockers
- likely next actions

## Core outputs
- Summaries
- Next-step suggestions
- Follow-up questions
- Contradiction warnings
- Pattern insights
- Project status snapshots

## Guidance inputs
- recent memories
- project state
- unfinished tasks
- graph structure
- connector imports
- AI outputs when needed

## Heuristics
- Prefer short, useful guidance.
- Never overwhelm the user.
- Prefer questions over assumptions.
- Show evidence for every recommendation.
- Prioritize active projects and current context.

## Example behaviors
- If a project has many notes but no tasks, suggest turning them into a plan.
- If a bug repeats across memories, surface the prior fix.
- If the user is in a long gap between updates, suggest resuming the project with the smallest next step.
- If thoughts are scattered, group them into one topic tree.

## Guidance quality rules
- Advice must be grounded in user data.
- Advice must be explainable.
- Advice must be actionable.
- Advice must be dismissible and configurable.
