# Clean-room development policy

[简体中文](CLEAN_ROOM.zh-CN.md)

This is an engineering policy, not legal advice.

## Allowed inputs

- Publicly observable behavior in released binaries and ordinary gameplay.
- Public user-facing documentation, command names, file formats, and network
  behavior when needed for interoperability.
- Minecraft and NeoForge documentation and APIs.
- Facts recorded in `FUNCTIONAL_SPEC.md` without implementation details.

## Prohibited inputs

- Copying, translating, decompiling, or adapting upstream source code.
- Copying art, textures, sounds, translations, prose, configuration defaults,
  lobby structures, logos, or screenshots. The LGPL-3.0 tier-list data identified
  in `NOTICE.md` is the sole documented exception.
- Reusing upstream package names, internal class names, or distinctive branding.
- Consulting upstream implementation code while implementing the corresponding
  subsystem.

## Contribution record

Every gameplay pull request must state:

1. Which functional-spec requirement it implements.
2. Which public behavior or independent design source was used.
3. That no prohibited upstream material was consulted or copied.
4. Whether any third-party asset or dependency was added and its license.

If a contributor has already studied upstream implementation code, they should
not implement the same subsystem. They may write a behavior-only test plan for
a separate implementer, omitting implementation details.

## Identity and compatibility

Use `neo_bingo` as the mod id and `dev.cleanroom.neobingo` as the package root.
Do not claim to be a port, official continuation, or drop-in replacement until
interoperability has been independently verified.
