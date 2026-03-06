# Kotlin + Spring Agent Skills

This repository is a code-free skill catalog for Kotlin + Spring development agents.

The canonical skill catalog lives in `.agents/skills/`.

To avoid duplicating the same 25 skill directories, `.claude/skills` is a symlink to that same catalog.

## Why The Repository Uses One Real Skill Root

Claude Code and Codex now converge on the same general **Agent Skills** model:

- each skill lives in its own directory
- the directory contains a required `SKILL.md`
- optional runtime-specific metadata and helper files may sit next to it

What differs is the directory that each runtime scans by default:

- Claude Code looks in `.claude/skills`
- Codex looks in `.agents/skills`

Anthropic documentation does not state that Claude Code directly scans `.agents/skills`.
Because of that, this repository keeps only one real copy of the skills in `.agents/skills` and exposes it to Claude Code through a symlink at `.claude/skills`.

## Repository Layout

```text
.agents/
  skills/
    <skill-name>/
      SKILL.md
      agents/openai.yaml

.claude/
  skills -> ../.agents/skills
```

## Usage

Clone the repository:

```bash
git clone <your-github-url>
cd kotlin-spring-agent-skills-test
```

Then:

- Codex reads the real catalog from `.agents/skills`
- Claude Code reads the same catalog through `.claude/skills`

If you want to use this repository as an external skill library instead of as the current project:

- Claude Code can load skills from additional directories
- Codex can load shared skills from additional directories or team config

See the documentation links below for the exact runtime behavior.

## Documentation

- Anthropic Claude Code Skills: <https://docs.anthropic.com/en/docs/claude-code/skills>
- OpenAI Codex Skills: <https://developers.openai.com/codex/skills>
- OpenAI official skills repository: <https://github.com/openai/skills>
- ChatGPT GPT Actions overview: <https://platform.openai.com/docs/actions/introduction/get-started-on-building>
- Agent Skills open standard: <https://agentskills.io/>

## Notes

- Tiering still exists conceptually, but the filesystem layout is runtime-native rather than tier-based.
- Every skill remains self-contained and reusable.
- `agents/openai.yaml` is kept with each skill because it is useful for OpenAI-side tooling and does not interfere with Claude Code skill loading.
- On the OpenAI side, repository-local `SKILL.md` discovery is documented for Codex. Generic ChatGPT custom GPTs use a different packaging model based on instructions, knowledge, and Actions rather than scanning a cloned repository for skills.
- I did not find official Anthropic documentation promising direct `.agents/skills` discovery. The symlink is there specifically to avoid depending on undocumented behavior.
