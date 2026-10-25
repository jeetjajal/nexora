package com.nexora.category.exception;

/**
 * Thrown when an ADMIN tries to create a category whose name is
 * already taken. Category.name has a unique database constraint
 * (see category/entity/Category.java, Phase 2) — this exception lets
 * us catch that situation explicitly and return a clean 409, rather
 * than letting a raw DataIntegrityViolationException bubble up.
 */
public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String name) {
        super("Category already exists: " + name);
    }
}
