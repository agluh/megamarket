package com.github.agluh.megamarket.model;

import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Offer {

    @EqualsAndHashCode.Include
    private final UUID id;

    private final UUID parentId;

    private final String name;

    private final long price;

    private final Instant date;
}
