
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  RestTemplate restTemplate = null;

  StockQuotesService stockQuotesService = null;

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }



  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    if (restTemplate == null) {
      this.restTemplate = new RestTemplate();
    } else
      this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(String provider, RestTemplate restTemplate) {
    if (restTemplate == null) {
      this.restTemplate = new RestTemplate();
    } else
      this.restTemplate = restTemplate;

      stockQuotesService = StockQuoteServiceFactory.INSTANCE.getService(provider, restTemplate);

  }


  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  // Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF

  public Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }


  public Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) {
    TiingoCandle[] candleData =
        this.restTemplate.getForObject(buildUri(symbol, from, to), TiingoCandle[].class);
    System.out.println(candleData);
    return Arrays.asList(candleData);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    // String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
    // + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate
        + "&endDate=" + endDate + "&token=" + getToken();
  }

  public String getToken() {
    return "793632abbe9ea80113dcca16c2d07f183b110df6";
  }

  public List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {

    TiingoCandle[] candleData = this.restTemplate.getForObject(
        buildUri(trade.getSymbol(), trade.getPurchaseDate(), endDate), TiingoCandle[].class);
    System.out.println(candleData);
    return Arrays.asList(candleData);

  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {

    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      List<Candle> candlesData =
      stockQuotesService.getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);

      double buyPrice = getOpeningPriceOnStartDate(candlesData);
      double sellPrice = getClosingPriceOnEndDate(candlesData);

      annualizedReturns.add(calculateReturn(endDate, portfolioTrade, buyPrice, sellPrice));

    }

    annualizedReturns.sort(getComparator());

    return annualizedReturns;
  }

  public static AnnualizedReturn calculateReturn(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {

    double totalReturn = (sellPrice - buyPrice) / buyPrice;

    double annualizedReturn =
        Math.pow(1 + totalReturn,
            (double) 1
                / ((double) (ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate)) / 365.24f))
            - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

}
