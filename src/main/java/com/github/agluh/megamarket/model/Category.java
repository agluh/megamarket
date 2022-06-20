package com.github.agluh.megamarket.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @EqualsAndHashCode.Include
    private final UUID id;

    private final UUID parentId;

    private final String name;

    @Setter
    private Long price;

    @Setter
    private Instant date;
}
