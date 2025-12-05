package pl.deniotokiari.tickerwire.models

/*
 Implemented
 NEWS, SEARCH: https://www.marketaux.com/documentation - 100 per day
 SEARCH, INFO: https://marketstack.com/ - 100 calls per month
 SEARCH, INFO: https://site.financialmodelingprep.com/developer/docs - 250 calls per day
 NEWS, SEARCH, INFO: https://www.alphavantage.co/support/#api-key - 25 calls per day
 NEWS, SEARCH, INFO: https://www.stockdata.org/documentation - 100 per day
 NEWS, SEARCH, INFO: https://finnhub.io/docs/api - 60 calls per minute
 NEWS, SEARCH, INFO: https://massive.com/docs/rest/stocks/overview - 5 calls per minute
 */

enum class Provider {
    MARKETSTACK,
    STOCKDATA,
    MARKETAUX,
    FINANCIALMODELINGPREP,
    FINNHUB,
    MASSIVE,
    ALPHAVANTAGE,
}
