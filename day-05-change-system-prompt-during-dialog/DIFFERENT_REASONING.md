# Different Reasoning Modes - Day 4

This document describes the new reasoning modes added in Day 4 of the AI Advent Challenge.

## Overview

Day 4 introduces two new AI reasoning modes that enhance the application's problem-solving capabilities:

1. **Step-by-Step Reasoning Mode** - Systematic problem breakdown
2. **Expert Panel Discussion Mode** - Multi-perspective analysis

These modes are accessed through the Settings screen's Response Mode dropdown, alongside existing modes (Normal, Structured JSON, Dialog).

## Step-by-Step Reasoning Mode

### Purpose
This mode instructs the AI to solve problems systematically, showing its reasoning process at each step. It's designed to help users understand complex solutions and learn from the AI's approach.

### When to Use
- Mathematical calculations
- Logical puzzles and deductions
- Analytical questions
- Complex decision-making processes
- Learning and understanding new concepts
- Debugging and troubleshooting

### Response Structure
The AI follows a consistent format:

```
**Understanding the Problem:**
[Restatement of what the user is asking]

**Given Information:**
- [List of known facts/data]

**Step-by-Step Solution:**

**Step 1: [Step title]**
[Explanation and work]

**Step 2: [Step title]**
[Explanation and work]

[Continue with more steps as needed...]

**Final Answer/Conclusion:**
[Clear, concise answer with key takeaways]
```

### Example

**User:** "If a train travels 120 km in 2 hours, how long will it take to travel 300 km?"

**AI Response:**
```
**Understanding the Problem:**
We need to find the time required for a train to travel 300 km, given its speed from a previous trip.

**Given Information:**
- Distance traveled: 120 km
- Time taken: 2 hours
- New distance to calculate: 300 km

**Step-by-Step Solution:**

**Step 1: Calculate the train's speed**
Speed = Distance / Time
Speed = 120 km / 2 hours = 60 km/h

**Step 2: Calculate time for new distance**
Time = Distance / Speed
Time = 300 km / 60 km/h = 5 hours

**Final Answer:**
The train will take **5 hours** to travel 300 km at its current speed of 60 km/h.
```

## Expert Panel Discussion Mode

### Purpose
This mode simulates a panel of 3-4 diverse experts who analyze a topic from different perspectives. It helps users see multiple viewpoints and understand trade-offs before reaching a well-rounded conclusion.

### When to Use
- Business decisions requiring multiple perspectives
- Career advice and life choices
- Technical architecture discussions
- Controversial or complex topics
- Any situation where diverse expertise would be valuable

### Expert Selection
The AI dynamically selects relevant experts based on the topic:

| Topic Type | Example Experts |
|------------|-----------------|
| Technical | Software Engineer, Security Expert, UX Designer, Product Manager |
| Business | Economist, Marketing Expert, Financial Analyst, Entrepreneur |
| Science | Researcher, Practitioner, Ethicist, Science Communicator |
| Life Decisions | Psychologist, Life Coach, Financial Advisor, Experienced Mentor |
| Creative | Artist, Critic, Historian, Commercial Expert |

### Response Structure

```
**Panel Introduction:**
[Brief introduction of the experts and their backgrounds]

---

**Expert 1 - [Role/Title]:** "[Name]"
[Their perspective, arguments, and recommendations]

**Expert 2 - [Role/Title]:** "[Name]"
[Their perspective, which may agree, disagree, or add nuance]

**Expert 3 - [Role/Title]:** "[Name]"
[Another unique viewpoint or synthesis of previous points]

**Expert 4 - [Role/Title] (if needed):** "[Name]"
[Final perspective or mediating voice]

---

**Panel Discussion Highlights:**
[Brief summary of key points of agreement and disagreement]

**Consensus Conclusion:**
[The unified recommendation acknowledging multiple perspectives]
```

### Example

**User:** "Should I learn Python or JavaScript first?"

**AI Response:**
```
**Panel Introduction:**
Today's panel features experts in software education and career development:

---

**Expert 1 - Senior Software Engineer:** "Alex Chen"
"Both languages are excellent choices, but I'd recommend Python for beginners. Its clean syntax and readability make it ideal for learning programming concepts without getting bogged down in syntax details. Python's versatility in data science, AI, and scripting also opens many career paths."

**Expert 2 - Web Development Lead:** "Sarah Miller"
"I respectfully disagree with Alex. JavaScript is the language of the web, and learning it first gives immediate, visual feedback when you build websites. Seeing your code come to life in a browser is incredibly motivating for new learners."

**Expert 3 - Career Coach & Tech Recruiter:** "Michael Thompson"
"Both Alex and Sarah make valid points, but I think the answer depends on your goals. If you want quick job opportunities, JavaScript dominates web development roles. If you're interested in data science, automation, or AI, Python is the clear winner."

---

**Panel Discussion Highlights:**
- Agreement: Both languages are excellent for beginners
- Disagreement: Which provides better learning experience
- Common ground: The best choice depends on personal goals

**Consensus Conclusion:**
The panel recommends considering your specific interests: **Choose Python** if you're drawn to data science, AI, or automation. **Choose JavaScript** if you're excited about web development. Most importantly, commit to learning one deeply before branching out.
```

## Technical Implementation

### Files Modified
- `domain/model/ResponseMode.kt` - Added STEP_BY_STEP and EXPERT_PANEL enum values
- `domain/model/AiSettings.kt` - Added STEP_BY_STEP_SYSTEM_PROMPT and EXPERT_PANEL_SYSTEM_PROMPT
- `data/repository/ChatRepositoryImpl.kt` - Added handling for new modes
- `presentation/chat/ChatStore.kt` - Added system messages for new modes
- `presentation/settings/SettingsScreen.kt` - Added descriptions for new modes

### System Prompts
The new modes use detailed system prompts that instruct the AI on:
- The exact format to follow
- Critical rules for consistency
- Examples of expected output
- Guidelines for tone and structure

### Mode Selection
All five response modes (Normal, Structured JSON, Dialog, Step-by-Step, Expert Panel) are mutually exclusive. Selecting a new mode:
1. Clears the previous system prompt
2. Adds a new system message to the chat
3. Prepares the AI context for the selected mode

## Best Practices

### Step-by-Step Mode
- Ask questions that have clear, logical solutions
- Best for questions with definitive answers
- Allow the AI to show intermediate calculations

### Expert Panel Mode
- Use for subjective or multi-faceted questions
- Great for decision-making scenarios
- The consensus provides a balanced viewpoint

## Comparison of All Modes

| Mode | Best For | Output Style |
|------|----------|--------------|
| Normal | General conversation | Natural, conversational |
| Structured JSON | Data extraction | JSON format with metadata |
| Dialog | Requirements gathering | One question at a time |
| Step-by-Step | Problem solving | Numbered steps with reasoning |
| Expert Panel | Decision making | Multi-perspective discussion |
