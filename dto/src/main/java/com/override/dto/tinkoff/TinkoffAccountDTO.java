package com.override.dto.tinkoff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TinkoffAccountDTO {
    private String investAccountName;
    private String investAccountId;
}
