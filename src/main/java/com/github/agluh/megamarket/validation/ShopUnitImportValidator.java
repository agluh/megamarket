package com.github.agluh.megamarket.validation;

import com.github.agluh.megamarket.dto.ShopUnitImport;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator for catalog item.
 */
@Component
@AllArgsConstructor
public class ShopUnitImportValidator implements
    ConstraintValidator<ValidShopUnitImport, ShopUnitImport> {

    @Override
    public boolean isValid(ShopUnitImport shopUnitImport,
            ConstraintValidatorContext constraintValidatorContext) {
        if (shopUnitImport.isCategory()) {
            return shopUnitImport.getPrice() == null;
        } else {
            return shopUnitImport.getPrice() != null && shopUnitImport.getPrice() >= 0;
        }
    }
}
