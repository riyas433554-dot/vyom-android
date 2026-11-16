package com.hermes.assistant

/**
 * Truthful capability inventory for Vyom. "Planned" and "reference" entries are
 * not executable integrations; only INTEGRATED entries may be described as available.
 */
internal object VyomCapabilityCatalog {
    enum class Status { INTEGRATED, PLANNED, REFERENCE_ONLY, REVIEW_REQUIRED }

    data class Entry(
        val id: String,
        val name: String,
        val category: String,
        val summary: String,
        val source: String,
        val status: Status
    )

    val entries = listOf(
        Entry("superpowers", "Superpowers", "Skill pack", "Spec-first planning, TDD, debugging, review and delivery workflows.", "obra/superpowers", Status.PLANNED),
        Entry("anthropic-skills", "Anthropic skills", "Skill pack", "Official reusable skills, document workflows and skill-authoring guidance.", "anthropics/skills", Status.PLANNED),
        Entry("karpathy-skills", "Karpathy skills", "Engineering guidance", "Simplicity, goal-driven work and careful code-change principles.", "multica-ai/andrej-karpathy-skills", Status.PLANNED),
        Entry("everything-claude-code", "Everything Claude Code", "Agent harness", "Engineering skills, memory patterns, security checks and workflow automation.", "affaan-m/everything-claude-code", Status.PLANNED),
        Entry("gustavo-skills", "Gustavo", "Skill pack", "Potential engineering skills collection; exact intended Gustavo repository is unresolved.", "GustavoGutierrez/engineering-skills OR GustavoCarreno/claude-skills", Status.REVIEW_REQUIRED),
        Entry("ponytail", "Ponytail", "Engineering guidance", "YAGNI and minimal-change coding guidance.", "DietrichGebert/ponytail", Status.PLANNED),
        Entry("caveman", "Caveman", "Context efficiency", "Concise response mode and token-efficiency techniques.", "JuliusBrussee/caveman", Status.PLANNED),
        Entry("graphify", "Graphify", "Codebase intelligence", "Builds a knowledge graph for codebase exploration and impact analysis.", "Graphify-Labs/graphify", Status.PLANNED),
        Entry("claude-mem", "Claude-mem", "Memory", "Persistent session context capture, compression and retrieval.", "thedotmack/claude-mem", Status.PLANNED),
        Entry("planning-with-files", "Planning-with-files", "Planning", "File-backed plans, task progress and durable working context.", "OthmanAdi/planning-with-files", Status.PLANNED),
        Entry("taste-skill", "Taste skill", "UI quality", "Frontend visual quality, design direction and anti-generic UI guidance.", "Leonxlnx/taste-skill", Status.PLANNED),
        Entry("ui-ux-pro-max", "UI/UX Pro Max", "UI/UX design", "Design-system intelligence, styles, palettes and UX patterns.", "nextlevelbuilder/ui-ux-pro-max-skill", Status.PLANNED),
        Entry("mattpocock", "Matt Pocock skills", "Engineering guidance", "Composable engineering skills and practical development workflows.", "mattpocock/skills", Status.PLANNED),
        Entry("wshobson", "Wshobson agents", "Agent and skill pack", "Specialized agents, orchestrators, skills and commands.", "wshobson/agents", Status.PLANNED),
        Entry("awesome-claude-skills", "Awesome Claude Skills", "Skill directory", "Curated index of community skills; discovery source, not a runtime.", "ComposioHQ/awesome-claude-skills", Status.REFERENCE_ONLY),
        Entry("awesome-claude-code", "Awesome Claude Code", "Tool directory", "Curated index of Claude Code tools, extensions and integrations.", "hesreallyhim/awesome-claude-code", Status.REFERENCE_ONLY),
        Entry("claude-plugins", "Claude plugins", "Plugin directory", "Official Claude Code plugin marketplace; plugin formats need adapter work.", "anthropics/claude-plugins-official", Status.PLANNED),
        Entry("repomix", "Repomix", "Codebase packaging", "Packages repository context into model-readable bundles.", "yamadashy/repomix", Status.PLANNED),
        Entry("context7", "Context7", "Documentation MCP", "Retrieves current library and framework documentation.", "upstash/context7", Status.PLANNED),
        Entry("serena", "Serena", "Code intelligence MCP", "Semantic code navigation and symbol-aware editing tools.", "oraios/serena", Status.PLANNED),
        Entry("firecrawl", "Firecrawl", "Web research MCP", "Web crawling, scraping and structured page extraction; service credentials may be required.", "firecrawl/firecrawl", Status.PLANNED),
        Entry("beads", "Beads", "Task memory", "Structured issue/task graph and durable dependency tracking.", "gastownhall/beads", Status.PLANNED),
        Entry("vibe-kanban", "Vibe Kanban", "Task orchestration UI", "Kanban-style agent task coordination; separate service/runtime required.", "BloopAI/vibe-kanban", Status.PLANNED),
        Entry("github-mcp", "GitHub MCP", "Repository MCP", "Connects to GitHub MCP for repository reads, isolated branch writes and pull requests; device execution is not yet verified.", "github/github-mcp-server", Status.INTEGRATED),
        Entry("playwright-mcp", "Playwright MCP", "Browser automation MCP", "Browser navigation, interaction and page verification; requires a reachable browser runtime.", "microsoft/playwright-mcp", Status.PLANNED),
        Entry("awesome-mcp-servers", "Awesome MCP Servers", "MCP directory", "Curated MCP server discovery list, not an installed server.", "punkpeye/awesome-mcp-servers", Status.REFERENCE_ONLY),
        Entry("claude-code-router", "Claude Code Router", "Model router", "Claude Code-compatible model routing proxy; not a replacement for Vyom's Gemma/Nemotron router.", "musistudio/claude-code-router", Status.PLANNED),
        Entry("cc-switch", "CC Switch", "Desktop configuration", "Desktop provider/profile switcher for coding-agent CLIs; Android adaptation needed.", "farion1231/cc-switch", Status.PLANNED),
        Entry("claude-hud", "Claude HUD", "CLI status UI", "Terminal status display for context, tools and task progress; requires UI adaptation.", "jarrodwatts/claude-hud", Status.PLANNED),
        Entry("rtk", "RTK", "CLI efficiency", "Command-output filtering and token reduction; requires a compatible shell runtime.", "rtk-ai/rtk", Status.PLANNED),
        Entry("headroom", "Headroom", "Context efficiency", "Compresses tool output and retrieved context before model input.", "chopratejas/headroom", Status.PLANNED),
        Entry("system-prompts", "System prompts", "Prompt reference", "Public prompt examples for study; never imported as trusted policy or executed blindly.", "x1xhlol/system-prompts-and-models-of-ai-tools", Status.REFERENCE_ONLY)
    )

    fun memoryFacts(): List<String> = listOf(
        "Vyom capability status: GitHub MCP is integrated in the current coding-agent branch for repository inspection, isolated branch commits and pull requests; Android-device execution and live end-to-end operation are not verified.",
        "Vyom planned skill packs: Superpowers, Anthropic skills, Karpathy skills, Everything Claude Code, Ponytail, Caveman, Graphify, Claude-mem, Planning-with-files, Taste skill, UI/UX Pro Max, Matt Pocock skills, Wshobson agents and Claude plugins. These are not installed yet.",
        "Vyom planned tools: Repomix, Context7, Serena, Firecrawl, Beads, Vibe Kanban, Playwright MCP, Claude Code Router, CC Switch, Claude HUD, RTK and Headroom. They are not installed or configured yet; each needs compatibility, runtime, license and credential review.",
        "Vyom reference-only directories: Awesome Claude Skills, Awesome Claude Code, Awesome MCP Servers and public system-prompt collections. They are discovery references, not executable powers.",
        "Gustavo skill source is unresolved: candidate repositories are GustavoGutierrez/engineering-skills and GustavoCarreno/claude-skills. Do not install either until the intended source is confirmed.",
        "Vyom must never claim a planned, reference-only or unverified capability as installed or working. Keep Gemma/Nemotron as the model route; do not silently add Claude or DeepSeek."
    )
}
