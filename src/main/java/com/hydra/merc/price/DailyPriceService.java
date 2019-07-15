package com.hydra.merc.price;

import com.hydra.merc.contract.Contract;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.Optional;

/**
 * Created By ahmed on 07-13-2019
 */
@Service
public class DailyPriceService {

    private final DailyPriceRepo dailyPriceRepo;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Autowired
    public DailyPriceService(DailyPriceRepo dailyPriceRepo) {
        this.dailyPriceRepo = dailyPriceRepo;
    }

    public Optional<DailyPrice> getPrice(Contract contract) {
        return getPrice(contract, LocalDate.now());
    }

    public Optional<DailyPrice> getPrice(Contract contract, LocalDate date) {
        return dailyPriceRepo.findDistinctByContractAndDay(contract, date);
    }


    public DailyPrice recordPrice(DailyPrice price) {
        return dailyPriceRepo.save(price);
    }
}
