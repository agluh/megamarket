package com.github.agluh.megamarket.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Marking annotation for catalog item validation.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ShopUnitImportValidator.class)
public @interface ValidShopUnitImport {

    String message() default "Importing data is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
