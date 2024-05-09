
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

   // TODO: CRIO_TASK_MODULE_REST_API
   // Find out the closing price of each stock on the end_date and return the list
   // of all symbols in ascending order by its close value on end date.

   // Note:
   // 1. You may have to register on Tiingo to get the api_token.
   // 2. Look at args parameter and the module instructions carefully.
   // 2. You can copy relevant code from #mainReadFile to parse the Json.
   // 3. Use RestTemplate#getForObject in order to call the API,
   // and deserialize the results in List<Candle>

   public static List<String> mainReadQuotes(String[] args)
         throws IOException, URISyntaxException, RuntimeException, InterruptedException {

      RestTemplate restTemplate = new RestTemplate();

      List<TotalReturnsDto> returns = new ArrayList<TotalReturnsDto>();

      List<String> symbols = new ArrayList<String>();

      List<PortfolioTrade> trade = null;

      trade = readTradesFromJson(args[0]);

      for (PortfolioTrade portfolioTrade : trade) {
         TimeUnit.SECONDS.sleep(5);
         TiingoCandle[] tingoData =
               restTemplate.getForObject(prepareUrl(portfolioTrade, LocalDate.parse(args[1]),
                     PortfolioManagerApplication.getToken()), TiingoCandle[].class);
         returns.add(new TotalReturnsDto(portfolioTrade.getSymbol(), tingoData[0].getClose()));
      }

      returns.sort((a, b) -> {
         if (a.getClosingPrice() < b.getClosingPrice())
            return -1;
         else if (a.getClosingPrice() > b.getClosingPrice())
            return 1;
         else
            return 0;
      });

      for (TotalReturnsDto totalReturn : returns) {
         symbols.add(totalReturn.getSymbol());
      }

      return symbols;


   }

   public static List<String> mainReadFile(String[] fileName)
         throws StreamReadException, DatabindException, IOException, URISyntaxException {
      List<String> symbols = new ArrayList<String>();
      List<PortfolioTrade> trade = null;

      for (String file : fileName) {
         trade = readTradesFromJson(file);
         for (PortfolioTrade portfolioTrade : trade) {
            symbols.add(portfolioTrade.getSymbol());
         }
      }

      return symbols;


   }

   public static List<String> debugOutputs() {
      return new ArrayList<String>(Arrays.asList("trades.json", "trades.json", "ObjectMapper", "mainReadFile"));
   }


   // TODO:
   // Build the Url using given parameters and use this function in your code to cann the API.
   public static String prepareUrl(PortfolioTrade trade, LocalDate args, String token) {
      return "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?startDate="
            + trade.getPurchaseDate() + "&endDate=" + args + "&token=" + token;

   }



   // TODO: CRIO_TASK_MODULE_CALCULATIONS
   // Now that you have the list of PortfolioTrade and their data, calculate annualized returns
   // for the stocks provided in the Json.
   // Use the function you just wrote #calculateAnnualizedReturns.
   // Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

   // Note:
   // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
   // 2. Remember to get the latest quotes from Tiingo API.



   // TODO:
   // Ensure all tests are passing using below command
   // ./gradlew test --tests ModuleThreeRefactorTest
   static Double getOpeningPriceOnStartDate(List<Candle> candles) {
      return candles.get(0).getOpen();
   }


   public static Double getClosingPriceOnEndDate(List<Candle> candles) {
      return candles.get(candles.size() - 1).getClose();
   }


   public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {

      RestTemplate restTemplate = new RestTemplate();
      TiingoCandle[] candleData =
            restTemplate.getForObject(prepareUrl(trade, endDate, token), TiingoCandle[].class);
      System.out.println(candleData);
      return Arrays.asList(candleData);

   }

   public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
         throws IOException, URISyntaxException {
            List<PortfolioTrade> trade = null;

            List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
      
            trade = readTradesFromJson(args[0]);
      
            for (PortfolioTrade portfolioTrade : trade) {
               List<Candle> candlesData = fetchCandles(portfolioTrade, LocalDate.parse(args[1]), PortfolioManagerApplication.getToken());

               double buyPrice = PortfolioManagerApplication.getOpeningPriceOnStartDate(candlesData);
               double sellPrice = PortfolioManagerApplication.getClosingPriceOnEndDate(candlesData);

               annualizedReturns.add(PortfolioManagerApplication.calculateAnnualizedReturns(LocalDate.parse(args[1]), portfolioTrade, buyPrice, sellPrice));
               
            }

            annualizedReturns.sort((a,b) -> {
               if (a.getAnnualizedReturn() < b.getAnnualizedReturn()) return 1;
               else if (a.getAnnualizedReturn() > b.getAnnualizedReturn()) return -1;
               else return 0;
            });

            return annualizedReturns;
   }

   // TODO: CRIO_TASK_MODULE_CALCULATIONS
   // Return the populated list of AnnualizedReturn for all stocks.
   // Annualized returns should be calculated in two steps:
   // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
   // 1.1 Store the same as totalReturns
   // 2. Calculate extrapolated annualized returns by scaling the same in years span.
   // The formula is:
   // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
   // 2.1 Store the same as annualized_returns
   // Test the same using below specified command. The build should be successful.
   // ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

   public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
         PortfolioTrade trade, Double buyPrice, Double sellPrice) {

            double totalReturn = (sellPrice - buyPrice) / buyPrice;

            double annualizedReturn = Math.pow(1 + totalReturn, (double) 1 / ((double) (ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate)) / 365.24f)) - 1; 

      return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
   }

   public static String getToken() {
      return "793632abbe9ea80113dcca16c2d07f183b110df6";
   }



   // public static void main(String[] args) throws Exception {
   //    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
   //    ThreadContext.put("runId", UUID.randomUUID().toString());



   //    printJsonObject(mainCalculateSingleReturn(args));

   // }

   // private static void printJsonObject(List<AnnualizedReturn> mainCalculate) {
   //    System.out.println(mainCalculate);
   // }



   public static ObjectMapper getObjectMapper() {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper;
   }


   public static List<PortfolioTrade> readTradesFromJson(String filename)
         throws IOException, URISyntaxException {
      // return Collections.emptyList();

      ObjectMapper objectMapper = getObjectMapper();

      PortfolioTrade[] trade = null;

      try {
         trade = objectMapper.readValue(new File("src/test/resources/" + filename),
               PortfolioTrade[].class);
      } catch (FileNotFoundException err) {
         trade = objectMapper.readValue(new File("src/main/resources/" + filename),
               PortfolioTrade[].class);
      }

      return new ArrayList<PortfolioTrade>(Arrays.asList(trade));

   }

   // TODO: CRIO_TASK_MODULE_REFACTOR
   // Once you are done with the implementation inside PortfolioManagerImpl and
   // PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
   // Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
   // call the newly implemented method in PortfolioManager to calculate the annualized returns.

   // Note:
   // Remember to confirm that you are getting same results for annualized returns as in Module 3.

   public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
         throws Exception {
      String file = args[0];
      LocalDate endDate = LocalDate.parse(args[1]);
      List<PortfolioTrade> portfolioTrades = readTradesFromJson(file);
      PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(null);
      return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
   }


   // public static void main(String[] args) throws Exception {
   //    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
   //    ThreadContext.put("runId", UUID.randomUUID().toString());



   //    printJsonObject(mainCalculateReturnsAfterRefactor(args));
   // }

   private static void printJsonObject(List<AnnualizedReturn> mainCalculate) {
      System.out.println(mainCalculate);
   }






















  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());




  }
}

