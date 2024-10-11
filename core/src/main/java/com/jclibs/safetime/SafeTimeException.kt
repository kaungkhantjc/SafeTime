package com.jclibs.safetime

import java.util.Locale

@Suppress("MemberVisibilityCanBePrivate")
/**
 * A custom exception class for handling errors related to SafeTime operations.
 *
 * This exception is used to report discrepancies between expected and actual values,
 * or to provide detailed error messages during SafeTime operations.
 *
 * @property property The name of the property where the error occurred, or `null` if not applicable.
 * @property expectedValue The expected value of the property that caused the exception.
 * @property actualValue The actual value of the property that caused the exception.
 */
class SafeTimeException : Exception {
    val property: String?
    val expectedValue: Int
    val actualValue: Int

    /**
     * Constructor for creating a [SafeTimeException] with just a detailed error message.
     *
     * @param detailMessage The error message describing what went wrong.
     */
    internal constructor(detailMessage: String) : super(detailMessage) {
        this.property = null
        this.expectedValue = 0
        this.actualValue = 0
    }

    /**
     * Constructor for creating a [SafeTimeException] with detailed error message and property details.
     *
     * This constructor is used when there is a mismatch between an expected and actual value for
     * a given property. The error message is formatted using the provided details.
     *
     * @param message The error message template, which includes placeholders for the property name, actual value,
     * and expected value.
     * @param property The name of the property that caused the error.
     * @param actualValue The actual value encountered during the operation.
     * @param expectedValue The value that was expected for the property.
     */
    internal constructor(
        message: String,
        property: String,
        actualValue: Int,
        expectedValue: Int
    ) : super(String.format(Locale.ENGLISH, message, property, actualValue, expectedValue)) {
        this.property = property
        this.actualValue = actualValue
        this.expectedValue = expectedValue
    }
}
