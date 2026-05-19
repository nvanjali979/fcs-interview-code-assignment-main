# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

**Challenges:**
Some costs like labor, inventory etc are easy to link to a specific Warehouse or Store. Others like
transportation, utilities etc are shared and need a fair split rule — for example, by order volume
or floor space. Choosing the wrong split method gives misleading cost reports.

**Key questions to clarify first:**
- How are costs captured today — ERP, spreadsheets, or both?
- What level of detail is needed: per Warehouse, per Store, per product?
- How quickly does the business need to see cost data — same day or end of month?

**Approach:**
Organise costs in a simple hierarchy: Location → Warehouse → Cost type. This lets you roll up
or drill down without double-counting. Track each cost event as it happens so the audit trail
is always complete and you can re-run reports if allocation rules change later.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

**How to identify opportunities:**
Start by measuring cost-per-order for each Warehouse and Store. That baseline reveals where
costs are highest. Usually a small number of locations drive most of the excess spend — focus
there first.

**Strategies worth considering:**
- Merge under-used Warehouses to cut fixed overheads.
- Reduce excess stock to lower storage costs.
- Optimise delivery routes to cut transport spend.
- Invest in automation for high-volume locations where the saving outweighs the upfront cost.

**How to prioritise:**
Rank each idea by expected saving, cost to implement, and disruption risk. Go after quick, low-risk
wins first, then use those savings to fund bigger structural changes. Always pilot at one location
before rolling out everywhere.

**Key questions:**
- Are there lease or contract lock-ins that limit consolidation options?
- How much service disruption is acceptable during a change?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

**Why it matters:**
Without integration, someone has to manually re-enter cost data into the financial system, which
is slow and error-prone. When the Cost Control Tool connects directly to the ERP/accounting system,
cost data flows automatically and both systems always agree.

**Main benefits:**
- No manual data entry or reconciliation work.
- Managers can see live cost vs. budget instead of waiting for month-end reports.
- A single audit trail that finance and operations both trust.

**How to keep it reliable:**
Agree on a shared data format (cost codes, currencies, periods) with the Finance team before
building anything. Use a reliable messaging approach so that if the connection drops, no cost
event is lost or duplicated. Run a daily check to confirm both systems match and alert on any gap.

**Key questions:**
- Which financial system is in use, and does it have an API?
- Is real-time sync required or is an end-of-day batch acceptable?
- Are there multiple currencies or legal entities involved?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

**Why it matters:**
Fulfillment costs change a lot — busy seasons, promotions, and carrier price changes all affect
the numbers. Without a budget and forecast, the business only finds out it overspent after the
fact. Good forecasting lets teams plan headcount, capacity, and purchasing in advance.

**What the system needs to support:**
- Store historical costs at a daily level so seasonal patterns are visible.
- Link costs to volume drivers (orders shipped, items stored) so forecasts adjust automatically
  when operational plans change.
- Send an alert when a Warehouse's spending is tracking above its budget, so managers can act
  before month-end.
- Allow "what-if" simulations — for example, "what does replacing this Warehouse do to our
  costs next quarter?"

**Key questions:**
- Is budgeting done per Warehouse, or at a higher company level?
- What time horizon is needed — quarterly, annual, multi-year?
- Who uses the forecast most — Finance, Operations, or both?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

**Why cost history must be kept:**
When a Warehouse is replaced, the old one is archived — not deleted. This matters for three
reasons:

1. **Compliance** — financial records must be kept for several years for audit and tax purposes.
   Deleting the old Warehouse would create a gap in the records.

2. **Performance comparison** — the new Warehouse serves the same area. Its costs can only be
   judged fairly by comparing them to what the old Warehouse actually spent. Without that history,
   there is no baseline.

3. **Budget setting** — the budget for the new Warehouse should be based on real data from its
   predecessor, adjusted for any expected improvements (e.g. better layout, cheaper carriers).

**How the system supports this:**
The system sets `archivedAt` on the old record instead of deleting it, and both records share the
same Business Unit Code. This means you can always query the full cost history of a business unit
in one go, or filter to only the current active Warehouse when you need operational figures.

**Key questions:**
- Are there unpaid invoices or open orders tied to the old Warehouse that need to be settled first?
- How should costs that arrive after the replacement date (e.g. a late invoice) be assigned?
- Does Finance need a formal closing entry when a Warehouse is archived?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
