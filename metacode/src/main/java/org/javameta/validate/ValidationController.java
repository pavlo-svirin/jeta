package org.javameta.validate;

import org.javameta.MasterController;
import org.javameta.metasitory.Metasitory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ValidationController extends MasterController<Object, ValidatorMetacode<Object>> {

    public ValidationController(Metasitory metasitory, Object master) {
        super(metasitory, master, Validate.class);
    }

    public void validate() throws ValidationException {
        List<String> errors = validateSafe();
        if (!errors.isEmpty())
            throw new ValidationException(errors);
    }

    public List<String> validateSafe() {
        List<String> errors = new ArrayList<>();
        for (ValidatorMetacode<Object> metacode : metacodes)
            errors.addAll(metacode.applyValidation(master));

        return errors;
    }

}