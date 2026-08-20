# BuboC — Late‑Stage Instrumentation Profiler for Graal JIT

BuboC is an experimental implementation of **late‑compiler‑stage instrumentation** for the [Graal](https://github.com/oracle/graal) just‑in‑time compiler.  
It injects probes only in the *low‑tier* Graal IR, after almost all high‑ and mid‑tier optimisations have run, so the collected profile resembles an uninstrumented run‑time execution. The idea originates from work by _**Optimization-Aware Compiler-Level Event Profiling**_ and _**Towards Realistic Results for Instrumentation-Based Profilers for JIT-Compiled Systems**_

---

## Why another profiler?

| Challenge with traditional profilers | How BuboC addresses it |
|-------------------------------------|------------------------|
| Probes inserted at bytecode or high‑tier IR interfere with inlining and other optimisations. | Probes are added in the final Graal low tier, so prior optimisations stay intact. |
| Instrument‑everything profilers often impose 80× overhead. | With an cycle threshold for “tiny” methods, BuboC shows a a much lower overhead on the AWFY suite |
| Tiny hot methods suffer when the probe is costlier than the code. | For units whose estimated cost is below 80 cycles, BuboC merely counts activations and estimates cycles from Graal’s own per‑node model |
| Sampling profilers avoid overhead but can miss short‑lived hot spots and suffer safepoint bias. | BuboC records nearly all cycles exactly (`RDTSC`), while still keeping low overhead. |

---

## How it works

1. **Metadata phase (high tier)** – gather compilation‑unit IDs and inlining maps; no probes yet.  
2. **Instrumentation phase (low tier)** – insert `ClockNode` before/after each compilation unit; fall back to lightweight counter probes for units below the threshold. 
3. **Accounting & report** – aggregate cycle deltas in a JVM‑side buffer; emit a plain‑text profile at JVM shutdown.

---

## Upstream

This repository is a functional fork of **[oracle/graal](https://github.com/oracle/graal)**.  

---
