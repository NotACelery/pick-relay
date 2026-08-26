# Pick Relay — Technical handoff 0.1.0-alpha.6

## Authoritative mining-mode decision

Pick Relay now has **two explicit session mining modes** chosen in the GUI before Start. Neither is a fallback for the other.

### Single Block

- Capture the exact block coordinate under the crosshair at Start.
- Only mine while the current normal-reach raycast resolves that coordinate.
- Looking elsewhere or temporary air pauses mining without stopping the session.
- Never redirect to the backing block or a different coordinate automatically.

### Line Mining

- Use THE Pick semantics.
- Raycast every cycle using normal player reach.
- Mine the current valid block under the crosshair.
- Camera rotation redirects mining immediately.
- Air/no valid block pauses attack but leaves the relay ACTIVE.

### Shared invariant

The only spatial safety anchor common to both modes is the **player position/dimension**. Any real displacement stops the session; camera rotation alone never does.

## Existing systems retained

- explicit 36-entry tool queue;
- Until Broken / Durability / Blocks budgets per entry;
- Preserve at 1;
- concrete tool tracking without injecting IDs into ItemStacks;
- hotbar/inventory relay with verified vanilla SWAP;
- selected-hotbar synchronization before mining;
- successful-destroy provenance guard;
- Unbreaking/Mending-aware observed durability consumption;
- physical gameplay mouse emergency stop;
- centralized cleanup and fail-safe Stop.

## Next validation

Use `docs/TESTING-alpha.6.md`. The highest-priority gameplay test is Single Block against a generator with a breakable backing structure, followed by Line Mining across several reachable blocks.
