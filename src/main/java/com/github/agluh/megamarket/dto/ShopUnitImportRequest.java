package com.github.agluh.megamarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ShopUnitImportRequest {
    @NotNull
    @Valid
    private List<ShopUnitImport> items;

    @NotNull
    @JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss[.SSS][XXX][XX][X]")
    private Instant updateDate;
}
