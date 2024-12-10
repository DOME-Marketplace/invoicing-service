package it.eng.dome.invoicing.tedb;

import java.io.IOException;
import java.time.Duration;
import java.util.Calendar;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import it.eng.dome.invoicing.tedb.model.Configurations;
import it.eng.dome.invoicing.tedb.model.SearchResult;
import it.eng.dome.invoicing.tedb.model.TaxRate;

public class TEDBCachedClient extends TEDBClient {

    private CacheManager cacheManager;

    private Cache<String, Configurations> configCache;
    private Cache<String, SearchResult> searchResultCache;
    private Cache<String, TaxRate> taxRateCache;

//    private TEDB client;

    public TEDBCachedClient() {
        super();
        this.initCaches();
    }

    public TEDBCachedClient(String url) {
        super(url);
        this.initCaches();
    }

    private void initCaches() {

        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        this.cacheManager.init();

        // size: 10, ttl: 5 days
        CacheConfiguration<String, Configurations> cconfig1 = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Configurations.class, ResourcePoolsBuilder.heap(10))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(5)))
                .build();
        this.configCache = this.cacheManager.createCache("configCache", cconfig1);

        // size: 100, ttl: 2 days
        CacheConfiguration<String, SearchResult> cconfig2 = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, SearchResult.class, ResourcePoolsBuilder.heap(100))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(1)))
                .build();
        this.searchResultCache = this.cacheManager.createCache("searchResultCache", cconfig2);

        // size: 100, ttl: 1 day
        CacheConfiguration<String, TaxRate> cconfig3 = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, TaxRate.class, ResourcePoolsBuilder.heap(100))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(1)))
                .build();
        this.taxRateCache = this.cacheManager.createCache("taxRateCache", cconfig3);

    }

    @Override
    public Configurations getConfigurations() throws IOException, InterruptedException {
        String key = "unique";
        if (!this.configCache.containsKey(key)) {
            Configurations configs = super.getConfigurations();
            this.configCache.put(key, configs);
        }
        return this.configCache.get(key);
    }

    @Override
    public SearchResult searchTaxes(String tedbCountryId, String taxType, Calendar date) throws IOException, InterruptedException {
        String key = tedbCountryId + taxType + date.get(Calendar.YEAR) + date.get(Calendar.DAY_OF_YEAR);
        if (!this.searchResultCache.containsKey(key)) {
            SearchResult sr = super.searchTaxes(tedbCountryId, taxType, date);
            this.searchResultCache.put(key, sr);
        }
        return this.searchResultCache.get(key);
    }

    @Override
    public TaxRate getTaxRate(String taxId, String versionDate) throws IOException, InterruptedException {
        String key = taxId + versionDate;
        if (!this.taxRateCache.containsKey(key)) {
            TaxRate sr = super.getTaxRate(taxId, versionDate);
            this.taxRateCache.put(key, sr);
        }
        return this.taxRateCache.get(key);
    }

}
