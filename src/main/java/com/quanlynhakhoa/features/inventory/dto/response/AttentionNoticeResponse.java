package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;

public record AttentionNoticeResponse(NoticeSeverity mucDo, String noiDung) {
}
