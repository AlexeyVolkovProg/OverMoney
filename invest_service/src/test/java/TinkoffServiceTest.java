import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.override.invest_service.config.MoexProperties;
import com.override.invest_service.dto.IMOEXDataDTO.IMOEXData;
import com.override.invest_service.dto.IMOEXDataDTO.IMOEXDataDTO;
import com.override.invest_service.dto.MarketTQBRDataDTO;
import com.override.invest_service.service.MOEXService;
import com.override.invest_service.service.TinkoffService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.util.ResourceUtils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TinkoffServiceTest {
    private final double TARGET_TOTAL_WEIGHT = 100d;
    private final String MARKET_TEST_DATA_FILEPATH = "classpath:marketTQBRDataDTOMap.json";

    @Mock
    private MOEXService moexService;

    @InjectMocks
    private TinkoffService tinkoffService;

    private Map<String, MarketTQBRDataDTO> marketTQBRDataDTOMap;
    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();

        try {
            marketTQBRDataDTOMap = objectMapper.readValue(
                    ResourceUtils.getFile(MARKET_TEST_DATA_FILEPATH),
                    new TypeReference<>() {
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Тест для проверки суммы весов в индексе,
     * сумма весов должна быть равна 100%
     */
    @ParameterizedTest
    @MethodSource("provideInvestAmounts")
    public void complianceToTotalWeightTest(double investAmount) throws IOException {
        Map<String, Double> tickerToWeight = buildRebalancedIMOEXIndexTest(investAmount);

        double totalWeight = tickerToWeight.values().stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(totalWeight, TARGET_TOTAL_WEIGHT, 0.1d);
    }


    /**
     * Тест для сравнения значений весов с референсными значениями, при определенной investAmount
     */
    @ParameterizedTest
    @MethodSource("provideInvestAmounts")
    public void complianceWeightsToReferenceValuesTest(double investAmount) throws IOException {
        String dataFilePath = String.format("%sreferenceData/tickerToWeight_%s.json",
                ResourceUtils.CLASSPATH_URL_PREFIX,
                investAmount);

        Map<String, Double> calculatedTickerToWeight = buildRebalancedIMOEXIndexTest(investAmount);
        Map<String, Double> referenceTickerToWeight = objectMapper.readValue(
                ResourceUtils.getFile(dataFilePath),
                new TypeReference<>() {
                });

        assertEquals(referenceTickerToWeight, calculatedTickerToWeight);
    }

    /**
     * Тест для проверки стоимости собранного портфеля на основе индекса,
     * стоимость не должна превышать заданную сумму инвестиции
     */
    @ParameterizedTest
    @MethodSource("provideInvestAmounts")
    public void portfolioValueInLimitAmountTest(double investAmount) throws IOException {
        Map<String, Double> tickerToWeight = buildRebalancedIMOEXIndexTest(investAmount);

        double currentSum = tickerToWeight
                .entrySet()
                .stream()
                .flatMapToDouble(tickerWeightPair -> {
                    int lots = marketTQBRDataDTOMap.get(tickerWeightPair.getKey()).getLots();
                    double priceForOne = marketTQBRDataDTOMap.get(tickerWeightPair.getKey()).getPrice();
                    double correctPrice = investAmount * tickerWeightPair.getValue() / 100;

                    int correctQuantity = (int) (correctPrice / priceForOne);
                    if (lots > 1) {
                        correctQuantity = correctQuantity - (correctQuantity % lots);
                    }
                    return DoubleStream.of(correctQuantity * priceForOne);
                }).sum();

        assertTrue(currentSum <= investAmount);
        assertTrue(0 < currentSum);
    }

    private Map<String, Double> buildRebalancedIMOEXIndexTest(double investAmount) throws IOException {
        Mockito.when(moexService.getTickerToWeight()).thenReturn(
                objectMapper.readValue(ResourceUtils.getFile(MoexProperties.IMOEX_DATA_FILENAME), IMOEXDataDTO.class)
                        .getAnalytics()
                        .getImoexData()
                        .stream()
                        .collect(Collectors.toMap(
                                IMOEXData::getSecIds,
                                IMOEXData::getWeight)));

        return tinkoffService.rebalanceIndexByAmount(
                moexService.getTickerToWeight(), marketTQBRDataDTOMap, investAmount);
    }

    private static Stream<Arguments> provideInvestAmounts() {
        return Stream.of(
                Arguments.of(30000),
                Arguments.of(50000),
                Arguments.of(500000),
                Arguments.of(5000000),
                Arguments.of(10000000),
                Arguments.of(30000000)
        );
    }
}