package org.kantega.missinglink.test;


import org.apache.commons.validator.EmailValidator;

public class ValidatingValidator {
    public boolean emailIsValid(){
        return EmailValidator.getInstance().isValid("marlil@kantega.no");
    }

    public static void main(String[] args) {
        boolean b = new ValidatingValidator().emailIsValid();
    }
}
