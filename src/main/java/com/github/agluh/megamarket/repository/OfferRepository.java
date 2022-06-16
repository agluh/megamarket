package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.Offer;
import java.util.Collection;
import java.util.UUID;

/**
 * Repository for offers.
 */
public interface OfferRepository {

    void save(Collection<Offer> offers);

    void delete(UUID offerId);
}
