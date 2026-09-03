package com.purchaseplatform.integration.treasury;

import java.util.List;

public record TreasuryExchangeRateResponse(
        List<TreasuryExchangeRate> data
) {
}