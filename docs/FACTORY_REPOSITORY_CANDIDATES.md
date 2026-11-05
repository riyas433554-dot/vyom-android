# Vyom Factory — owner-supplied repository candidates

Date received: 2026-11-05

This is a discovery queue, not an import manifest. Rank and names are copied from the owner's supplied snapshot; stars, forks, last-commit dates, ownership, current license and current repository state have not been independently verified here.

**Safety contract**
- Every entry must be resolved to one canonical `https://github.com/owner/repository` URL before inspection.
- Inspect the repository at an immutable commit SHA; record its license and archived/tree-truncated status.
- Classify the repository by contents (source, API spec, workflow, docs, skill, etc.). Do not assume every repository is a `SKILL.md`.
- No repository code, build script, workflow, agent prompt or tool is executed during intake.
- Imported skills remain inert knowledge. Any executable capability requires a separate reviewed adapter, schema, permission policy, tests, approval and rollback.
- Ambiguous names (for example `skills`, `pi`, `ui`, `rea`) require owner/repository disambiguation.
- `deepseek-harness` is explicitly excluded from import because the owner has ruled out DeepSeek.

## Candidate ranking

| Rank | Repository name | Initial handling |
|---:|---|---|
| 1 | build-your-own-x | Reference/docs review |
| 2 | awesome | Curated knowledge review |
| 3 | public-apis | API catalogue review |
| 4 | freeCodeCamp | Learning/reference review |
| 5 | rea | Agent/reverse-engineering review; resolve owner |
| 6 | free-programming-books | Curated knowledge review |
| 7 | openclaw | Agent architecture review |
| 8 | system-design-primer | Engineering reference review |
| 9 | developer-roadmap | Planning/reference review |
| 10 | coding-interview-university | Learning/reference review |
| 11 | awesome-selfhosted | Tool catalogue review |
| 12 | awesome-python | Python ecosystem catalogue |
| 13 | skills | Skill format/source review; resolve owner |
| 14 | superpowers | Agent workflow/skill review |
| 15 | ECC | Agent harness review |
| 16 | project-based-learning | Learning/reference review |
| 17 | 996.ICU | Low Factory relevance; defer |
| 18 | deepseek-harness | **EXCLUDED — DeepSeek is not to be used** |
| 19 | hermes-agent | Agent architecture review |
| 20 | the-book-of-secret-knowledge | Curated knowledge review |
| 21 | linux | OS reference; defer |
| 22 | react | UI framework reference |
| 23 | andrej-karpathy-skills | Skill/reference review |
| 24 | Python | Language reference; defer |
| 25 | opencode | Coding-agent architecture review |
| 26 | vue | UI framework reference |
| 27 | computer-science | Learning/reference review |
| 28 | n8n | Workflow automation review |
| 29 | FreeDomain | Low Factory relevance; defer |
| 30 | yt-dlp | Media utility; defer |
| 31 | tensorflow | ML framework reference; defer |
| 32 | firecrawl | Web extraction/tool adapter review |
| 33 | Microsoft-Activation-Scripts | Defer; unrelated and elevated trust review |
| 34 | javascript-algorithms | Learning/reference review |
| 35 | claw-code | Autonomous coding-agent review |
| 36 | vscode | Developer-tool reference; defer |
| 37 | markitdown | Document-ingestion adapter review |
| 38 | awesome-go | Go ecosystem catalogue |
| 39 | ohmyzsh | Developer-tool reference; defer |
| 40 | AutoGPT | Agent architecture review |
| 41 | Python-100-Days | Learning/reference review |
| 42 | skills | Agent skills catalogue; resolve owner separately from rank 13 |
| 43 | HelloGitHub | Curated project discovery |
| 44 | CS-Notes | Learning/reference review |
| 45 | ollama | Model-runtime integration research; exclude DeepSeek models |
| 46 | You-Dont-Know-JS | Learning/reference review |
| 47 | ponytail | Agent engineering workflow review |
| 48 | flutter | UI framework reference; defer |
| 49 | gitignore | Developer-tool data review |
| 50 | prompts.chat | Prompt catalogue review; treat prompts as untrusted data |
| 51 | bootstrap | UI framework reference |
| 52 | transformers | Model framework reference; defer |
| 53 | agency-agents | Agent-role reference review |
| 54 | stable-diffusion-webui | Media tooling; defer |
| 55 | the-art-of-command-line | Developer reference review |
| 56 | dify | Agent/workflow platform review |
| 57 | JavaGuide | Learning/reference review |
| 58 | open-webui | AI interface/tooling review |
| 59 | clash-verge-rev | Unrelated desktop utility; defer |
| 60 | langflow | Agent workflow review |
| 61 | claude-code | Coding-agent architecture review |
| 62 | scrcpy | Android device tooling review |
| 63 | cc-switch | Model/tool routing reference review |
| 64 | langchain | Agent framework reference review |
| 65 | javascript | Language reference; defer |
| 66 | spec-kit | Specification/planning workflow review |
| 67 | ui-ux-pro-max-skill | UI skill/reference review |
| 68 | ComfyUI | Media workflow tooling; defer |
| 69 | system-prompts-and-models-of-ai-tools | Prompt/security research; no prompt execution |
| 70 | awesome-llm-apps | Agent/app pattern catalogue |
| 71 | next.js | UI framework reference; defer |
| 72 | iptv | Unrelated; defer |
| 73 | tech-interview-handbook | Learning/reference review |
| 74 | graphify | Repository/code intelligence review |
| 75 | free-for-dev | Service catalogue review |
| 76 | youtube-dl | Media utility; defer |
| 77 | PowerToys | Unrelated desktop utility; defer |
| 78 | go | Language reference; defer |
| 79 | gstack | Agent workflow review |
| 80 | MoneyPrinterTurbo | Media generation; defer |
| 81 | fucking-algorithm | Learning/reference review |
| 82 | excalidraw | Diagram/design tooling review |
| 83 | llama.cpp | Model-runtime reference; defer |
| 84 | codex | Coding-agent architecture review |
| 85 | hello-algo | Learning/reference review |
| 86 | 30-seconds-of-code | Code reference review |
| 87 | kubernetes | Infrastructure reference; defer |
| 88 | rustdesk | Unrelated remote-desktop tool; defer |
| 89 | ui | UI library; resolve owner |
| 90 | react-native | Android/UI framework reference |
| 91 | awesome-design-md | Design-system knowledge review |
| 92 | Awesome-Hacking | Security knowledge; review only, no exploit execution |
| 93 | electron | Desktop framework reference; defer |
| 94 | node | Runtime reference; defer |
| 95 | generative-ai-for-beginners | Learning/reference review |
| 96 | rust | Language reference; defer |
| 97 | TradingAgents | Financial agent; defer |
| 98 | pi | Agent toolkit; resolve owner |
| 99 | browser-use | Browser automation architecture review |
| 100 | v2rayN | Unrelated networking utility; defer |

## Current import status

- Canonical URLs resolved: 0/100
- Immutable commits inspected: 0/100
- Licenses reviewed: 0/100
- Imported/installed: 0/100
- Executable capabilities granted: 0

The first useful integration candidates are the agent harness/workflow projects, repository intelligence, browser tooling, document ingestion, API catalogues and design-system references. The intake engine must assess each one before any installation or adapter work.
