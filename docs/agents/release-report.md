# Owner review report

Use the `$html` skill to create the review as soon as a requested release starts,
then keep it current through implementation, testing and delivery. Read its destination repository's
instructions and save a separate project document for that version, such as
`main--4.4.0-review.html`. Keep earlier reports as historical records.

Replacement history belongs only in this private report. Public changelogs and
GitHub notes fold all changes into the existing release without replacement labels;
GitHub retains no public original or superseded release/tag copies.

For a re-release, update that version's existing report. Mark every added or revised
item with a visible **Re-released** label and a short note explaining the revision.
Keep this label separate from development status so it remains visible after
Complete labels are hidden. Refresh affected descriptions, screenshots and
verification results, and add a timestamped Work log entry. Preserve the report's
identity, checklist IDs and owner's saved review progress.

At the top, include a collapsed **Work log** with timestamped milestone updates.
Record high-level progress, the number and roles of subagents, completed work and
what remains. Keep entries short and distinguish code completion, review and tests.
Update this as work progresses so the owner can give feedback before delivery.

When a visible feature is close to final, take a basic screenshot in the disposable
client and add it before running its acceptance tests. Capture only key UI changes;
skip behavior that a static image cannot show. Never interrupt the owner's instance.
Store captures with the private HTML report, not in the public modpack repository.
Use mostly large-screen captures in the owner report, with only one or two small-screen
examples. Give small layouts equal attention in native testing and independent UI review.
Give each applicable review item a small, subtle button that reveals its related
screenshots. Leave screenshots hidden by default, with concise captions and responsive
images. Do not attach unfinished mockups or imply a screenshot proves runtime behavior.

Show a short status next to every review item and update it whenever work starts,
code lands, checks run, review finishes or a problem reopens the item:

- **Planned**: derived from the request; no work started.
- **In Progress**: investigation, implementation or other work has started.
- **Built, untested**: code is complete; verification and review have not started.
- **Built, verified**: code and initial checks passed. Similar Built states may name
  the actual remaining step, such as Built, awaiting native checks.
- **Reviewed · MODEL**: identify the model that reviewed the implementation.
- **Reviewed, pending NEXT STEP**: review passed but a named step remains, such as
  native checks, final suites or deployment. Review alone is not verification.
- **Fixing**: checks or review found an issue and work has resumed.
- **Ready to deploy**: website implementation and checks are done, but production
  has not been updated. Never label local completion as deployed.
- **Deployed**: production was updated and its live behavior was checked.
- **Complete**: the item is finished. Show this status in green during development;
  hide Complete labels once the whole release is delivered to reduce clutter.

Show compact ownership beside each item: implementer and reviewer role, model and
reasoning level. Include the main agent too. Use actual session/task metadata rather
than guessing; mark unavailable details as unknown. State the next required step
separately so status, ownership and remaining work are easy to read.

Use consistent semantic colors: Planned grey; In Progress yellow; Built but unverified
blue, including verified code with remaining delivery work; Complete bright green;
reviewed but pending work muted green; Fixing or other issues orange/red. Reserve
the bright completion green for finished items, including verified live deployment.
Reviewed items with pending work retain an explicit pending label. Website delivery
states distinguish Ready to deploy from Deployed even when implementation is done.
Keep color supplemental to readable text, with enough contrast in the report theme.

Use the latest evidenced state, never a blanket optimistic label. Keep review and
verification details in the item's short note or the Work log when both matter.

Make a compact checklist the owner can read from top to bottom while testing:

- Group changes by the user's categories, in their requested order.
- Cover every requested point, including small visual and default changes.
- Give each point a short result. "Implemented" is enough when no decision needs explanation.
- State material decisions, migrations, differences and anything unfinished plainly.
- Include the exact names of removed settings and what replaces them.
- Add optional interactive checkboxes that remember this report's review progress locally.
- Keep technical notes brief. Explain practical behavior before implementation details.
- End with concise validation, release and instance-update status. Do not claim unrun tests.
  Record the primary update attempt before publication. If the instance was running
  or unavailable, keep its update visibly pending while publication proceeds; do not
  label the instance updated merely because GitHub and the website are live.

Do not repeat the prompt or turn the file into a session diary. Avoid long tables,
performance claims without measurements, filler and explanatory text that adds no
review value. Keep private paths, account details, logs and reports out of the public
modpack repository. Commit and push the HTML report and its screenshots to ai-html,
following that library's metadata and verification rules. Keep the owner's existing review progress.
