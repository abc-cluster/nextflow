# Unified constructor notation for process inputs and outputs

- Authors: Paolo Di Tommaso
- Status: proposed
- Deciders: Paolo Di Tommaso, Ben Sherman
- Date: 2026-03-12
- Updated: 2026-03-20
- Tags: lang, records, tuples, syntax

Technical Story: Follow-up to [Record types ADR](20260306-record-types.md)

## Summary

The current record types implementation uses two different syntactic forms for records in process inputs (block syntax) vs outputs (function-call syntax). This RFC proposes a **uniform constructor notation** — `name = constructor(...)` with optional type annotation — that applies to both `record()` and `tuple()` across inputs and outputs. This establishes a single syntactic pattern for all structured types in process definitions, provides a natural migration path from tuples to records, and ensures consistency across the language.

## Problem Statement

The accepted record types ADR ([20260306-record-types](20260306-record-types.md)) introduces two distinct syntactic forms for records within process definitions:

**Input** — a `Record { ... }` block syntax unique to inputs:
```nextflow
process FASTQC {
    input:
    sample: Record {
        id: String
        fastq_1: Path
        fastq_2: Path
    }
    ...
}
```

**Output** — a `record()` function call:
```nextflow
process FASTQC {
    ...
    output:
    record(id: sample.id, html: file('*.html'), zip: file('*.zip'))
}
```

This asymmetry means the same concept (a record) is expressed with two different syntactic forms depending on context. The block syntax `Record { ... }` exists only in process input declarations and has no counterpart elsewhere in the language. Meanwhile, the `record()` function call used in outputs is already a general-purpose construct usable in any expression context.

## Goals

- **Uniform constructor notation** — establish `name = constructor(...)` as the single syntactic pattern for all structured types (`record()` and `tuple()`) in process inputs and outputs.
- **Syntactic consistency** — use the same notation for records and tuples across inputs and outputs, eliminating context-dependent forms.
- **Alignment with existing syntax** — reuse assignment (`=`) and type annotation (`: Type`) patterns already present in process I/O, rather than introducing new block syntax.
- **Migration continuity** — provide a natural upgrade path from `tuple()` to `record()` by keeping the notation identical, so users only change the keyword to gain named-field semantics.
- **Standard type semantics** — record and tuple assignments should follow the same type compatibility rules as any other typed assignment in the language.

## Non-goals

- Changing the top-level `record` type definition syntax — the `record Name { field: Type }` declaration form is a type-level construct and is not affected by this proposal.
- Changing the `record()` function runtime behavior or the `RecordMap` implementation.
- Removing support for external type references (e.g. `sample: Sample`).
- Changing the runtime behavior of tuples — tuples retain their positional semantics.

## Considered Options

### Option 1: Current syntax (status quo)

Input uses a dedicated block syntax, output uses the `record()` function call:

```nextflow
process FASTQC {
    input:
    sample: Record {
        id: String
        fastq_1: Path
        fastq_2: Path
    }

    output:
    record(id: sample.id, html: file('*.html'), zip: file('*.zip'))
}
```

- Good, because input block syntax mirrors the top-level `record` definition.
- Bad, because two different notations for the same concept in the same process definition.
- Bad, because `Record { ... }` block syntax only exists in input declarations — it is not a general-purpose construct.

### Option 2: Block syntax for both inputs and outputs

Use `record { ... }` blocks in both input and output:

```nextflow
process FASTQC {
    input:
    record sample {
        id: String
        fastq_1: Path
        fastq_2: Path
    }

    output:
    record {
        id: String = sample.id
        html: Path = file('*.html')
        zip: Path = file('*.zip')
    }
}
```

- Good, because symmetric — same block form on both sides.
- Bad, because the output block mixes type declarations with value assignments (`Path = file(...)`).
- Bad, because block syntax in process I/O diverges from the function-call style already established for `record()`.

### Option 3: Uniform constructor notation for `record()` and `tuple()`

Establish `name = constructor(...)` as the single syntactic pattern for all structured types in process I/O. Both `record()` and `tuple()` follow the same three-tier notation — bare, assignment, and typed assignment:

**Record:**

```nextflow
// bare — anonymous output
record(id: sample.id, html: file('*.html'))

// assignment
result = record(id: sample.id, html: file('*.html'))

// typed assignment
result: QcResult = record(id: sample.id, html: file('*.html'))
```

**Tuple:**

```nextflow
// bare — anonymous output
tuple(id, file('*.bam'))

// assignment
out = tuple(id, file('*.bam'))

// typed assignment
out: Tuple<String,Path> = tuple(id, file('*.bam'))
```

The same pattern applies uniformly to inputs:

```nextflow
process FASTQC {
    input:
    sample = record(id: String, fastq_1: Path, fastq_2: Path)

    output:
    result = record(id: sample.id, html: file('*.html'), zip: file('*.zip'))
}
```

```nextflow
process ALIGN {
    input:
    in = tuple(id: String, fastq: Path)

    output:
    out = tuple(id, file('*.bam'))
}
```

With optional explicit type annotations:

```nextflow
process FASTQC {
    input:
    sample: Sample = record(id: String, fastq_1: Path, fastq_2: Path)

    output:
    result: QcResult = record(id: sample.id, html: file('*.html'), zip: file('*.zip'))
}
```

- Good, because same notation on both sides — `name = constructor(...)` — for both `record()` and `tuple()`.
- Good, because establishes a uniform constructor notation across all structured types.
- Good, because reuses existing assignment and type annotation patterns.
- Good, because `record()` and `tuple()` are already general-purpose functions, no new syntax needed.
- Good, because type annotations follow standard rules — `sample: Sample = record(...)` works like any typed assignment.
- Good, because the migration from `tuple()` to `record()` requires only changing the keyword — the notation is identical.
- Bad, because input `record()` and `tuple()` arguments are types rather than values, which is a different usage of the function.

## Solution or decision outcome

**Option 3**: Establish a **uniform constructor notation** — `name: Type = constructor(...)` — that applies to both `record()` and `tuple()` across process inputs and outputs. This eliminates the need for context-specific syntax forms and provides a natural migration path from tuples to records.

## Rationale & discussion

### Uniform constructor notation

The key insight is that both `record()` and `tuple()` are constructors, and everything else is standard Nextflow assignment and type annotation. This establishes a single syntactic pattern for all structured types in process definitions:

```
name = constructor(...)               // assignment
name: Type = constructor(...)         // typed assignment
constructor(...)                      // bare (anonymous output)
```

This pattern applies uniformly regardless of:
- **Constructor type** — `record()` or `tuple()`
- **Context** — input or output
- **Whether a type annotation is present**

No dedicated block syntax is needed. No context-dependent forms exist. Every structured input or output follows the same shape.

### Syntax pattern

The unified pattern is `name: Type = constructor(...)` for both inputs and outputs, for both records and tuples:

- **Record input**: `sample = record(id: String, fastq_1: Path, fastq_2: Path)` — declares the fields and their types being received.
- **Record output**: `result = record(id: sample.id, html: file('*.html'))` — declares the fields and their values being produced.
- **Tuple input**: `in = tuple(id: String, fastq: Path)` — declares the components and their types being received.
- **Tuple output**: `out = tuple(id, file('*.bam'))` — declares the components and their values being produced.

The only difference is what goes inside the constructor call — types on input (declaring structure), expressions on output (producing values). This parallels how assignment works elsewhere: the left side declares, the right side provides.

### Tuple and record: same notation, different semantics

The notation is identical for both constructors. The semantic difference is positional vs named:

| | `tuple()` | `record()` |
|---|---|---|
| Field access | Positional (`in[0]`) and named (`in.id`) | Named only (`sample.id`) |
| Order | Significant | Not significant |
| Duck typing | No | Yes |
| Extra fields | No | Yes (structural subtyping) |

This means migrating from tuple to record requires only changing the keyword — the surrounding notation stays the same:

```nextflow
// Tuple — positional semantics
in = tuple(id: String, fastq: Path)

// Record — named semantics (just change the keyword)
in = record(id: String, fastq: Path)
```

### Continuity with current tuple syntax

The typed process syntax already uses `tuple()` as a function-call constructor in outputs:

```nextflow
// Current typed output syntax
bam = tuple(id, file('*.bam'))
bai = tuple(id, file('*.bai'))
```

Option 3 extends this established pattern to inputs and applies the same pattern to `record()`. Users who already write `tuple()` in outputs understand the idiom — `record()` works the same way.

The migration path from classic DSL2 through the unified notation is:

```nextflow
// Classic DSL2
tuple val(id), path(fastq)

// Typed — uniform constructor notation
in = tuple(id: String, fastq: Path)

// Record — upgrade to named semantics when ready
in = record(id: String, fastq: Path)
```

Each step adds expressiveness without breaking the previous mental model.

### Type annotations

Type annotations are optional and follow standard semantics:

```nextflow
// Inferred type from record fields
sample = record(id: String, fastq_1: Path, fastq_2: Path)

// Explicit type — compiler checks compatibility with Sample
sample: Sample = record(id: String, fastq_1: Path, fastq_2: Path)

// Inferred type from tuple components
in = tuple(id: String, fastq: Path)

// Explicit type
in: Tuple<String,Path> = tuple(id: String, fastq: Path)
```

This is the same as writing `x: Integer = 42` vs `x = 42` — nothing constructor-specific about the assignment semantics.

### Alignment with existing process syntax

The proposed syntax reuses patterns that already exist in Nextflow process definitions:

| Existing pattern | Example | Constructor equivalent |
|-----------------|---------|----------------------|
| Scalar type annotation | `id: String` | `sample: Sample` |
| Assignment in output | `id = sample.id` | `result = record(...)` / `out = tuple(...)` |
| Typed assignment in output | `id: String = sample.id` | `result: QcResult = record(...)` / `out: Tuple<String,Path> = tuple(...)` |

### External type reference

When using a pre-defined record type, the syntax naturally simplifies:

```nextflow
// With inline fields
sample: Sample = record(id: String, fastq_1: Path, fastq_2: Path)

// With external type only (no inline fields needed)
sample: Sample
```

The `sample: Sample` shorthand remains valid — the `record()` call is only needed when defining fields inline.

### Full example

```nextflow
nextflow.preview.types = true

record Sample {
    id: String
    fastq_1: Path
    fastq_2: Path
}

process TOUCH {
    input:
    id: String

    output:
    result = record(id: id, fastq_1: file('*_1.fastq'), fastq_2: file('*_2.fastq'))

    script:
    """
    touch ${id}_1.fastq
    touch ${id}_2.fastq
    """
}

process FASTQC {
    input:
    sample: Sample = record(id: String, fastq_1: Path, fastq_2: Path)

    output:
    result = record(id: sample.id, html: file('*.html'), zip: file('*.zip'))

    script:
    """
    touch ${sample.id}.html
    touch ${sample.id}.zip
    """
}

workflow {
    ch_samples = TOUCH(channel.of('a', 'b', 'c'))
    ch_fastqc = FASTQC(ch_samples)
    ch_fastqc.view()
}
```

### Tuple and record coexistence

A process can use both tuples and records, with the same notation throughout:

```nextflow
process ALIGN {
    input:
    sample = record(id: String, fastq_1: Path, fastq_2: Path)

    output:
    result = record(id: sample.id, bam: file('*.bam'), bai: file('*.bai'))
}

process QUANT {
    input:
    in = tuple(id: String, bam: Path, bai: Path)

    output:
    out = tuple(id, file('quant'))

    script:
    """
    quant ${bam} ${bai} -o quant
    """
}
```

## Links

- Supersedes input syntax in [Record types ADR](20260306-record-types.md)
- Related: [Record types syntax summary](../plans/record-types-syntax-new.md)
