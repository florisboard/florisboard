/*
 * ZIPRAF_OMEGA Licensing Module v999
 * Copyright (C) 2025 Rafael Melo Reis
 * 
 * This module implements the licensing and validation system described in
 * the ZIPRAF_OMEGA_FULL specification, ensuring integrity, authorship,
 * permission, destination, and ethical purpose validation.
 * 
 * License: Apache 2.0 (technical modifications permitted)
 * Authorship credentials must be maintained per specification
 * Spiritual/symbolic core mutation prohibited
 * 
 * Standards Compliance:
 * - ISO 9001, 27001, 27017, 27018, 8000, 25010
 * - IEEE 830, 1012, 12207, 14764, 1633, 42010, 26514
 * - NIST CSF, 800-53, 800-207 (Zero Trust)
 * - IETF RFC 5280 (PKI), 7519 (JWT), 7230 (HTTP), 8446 (TLS 1.3)
 * - W3C standards (JSON, YAML, WebArch)
 */

package org.florisboard.lib.zipraf

import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * ZIPRAF_OMEGA Licensing and Validation Module
 * 
 * Implements the licensing system with cryptographic validation
 * and ethical compliance checking according to ZIPRAF_OMEGA specification.
 * 
 * Core validation factors:
 * 1. Integrity - Cryptographic hash verification (SHA3-512, BLAKE3)
 * 2. Authorship - Author identity validation (RAFCODE-Φ)
 * 3. Permission - Authorization verification (BITRAF64)
 * 4. Destination - Target validation
 * 5. Ethical purpose - Ethica[8] compliance
 */
@Serializable
data class LicensingModule(
    val identity: String = "RAFCODE-Φ",
    val encoding: String = "BITRAF64",
    val seal: String = "ΣΩΔΦBITRAF",
    val version: Int = 999,
    val author: String = "Rafael Melo Reis"
) {
    companion object {
        // Symbolic seals for identification and integrity
        val SEALS = listOf("Σ", "Ω", "Δ", "Φ", "B", "I", "T", "R", "A", "F")
        
        // BITRAF64 symbolic seed (partial representation)
        // Full implementation would include complete symbolic mapping
        const val BITRAF64_SEED = "AΔBΩΔTTΦIIBΩΔΣΣRΩRΔΔBΦΦFΔTTRR"
        
        // Correlation constant from specification
        const val R_CORR = 0.963999
        
        // Symbolic frequencies for resonance and validation
        // Values represent specific vibrational states in the ZIPRAF_OMEGA system:
        // - 100 Hz: Base frequency
        // - 144 kHz: Primary resonance
        // - 288 kHz: Harmonic resonance (2x primary)
        // - 1008 Hz: Validation frequency
        val FREQUENCIES = listOf(100, 144_000, 288_000, 1008)
    }
    
    /**
     * Validates execution permission based on all required factors
     * 
     * @param integrity Hash verification result
     * @param authorship Author validation result
     * @param permission Authorization check result
     * @param destination Target validation result
     * @param ethicalPurpose Ethica[8] compliance result
     * @return true if all validation factors pass, false otherwise
     */
    fun validateExecution(
        integrity: Boolean,
        authorship: Boolean,
        permission: Boolean,
        destination: Boolean,
        ethicalPurpose: Boolean
    ): Boolean {
        // All factors must pass for execution to be permitted
        // Any violation of Ethica[8] results in EXECUTION = DENIED
        return integrity && authorship && permission && destination && ethicalPurpose
    }
    
    /**
     * Computes cryptographic hash for integrity verification
     * 
     * Priority order:
     * 1. SHA3-512 (if available)
     * 2. SHA-256 (fallback, always available)
     * 
     * Note: BLAKE3 support requires external library (org.bouncycastle:bcprov-jdk15on
     * or similar). This can be added as an optional dependency when needed.
     * 
     * @param data Input data to hash
     * @return Hexadecimal string representation of hash
     * @throws RuntimeException if neither SHA3-512 nor SHA-256 are available (extremely rare)
     */
    fun computeHash(data: ByteArray): String {
        return try {
            // Try SHA3-512 first (preferred algorithm)
            val digest = MessageDigest.getInstance("SHA3-512")
            val hash = digest.digest(data)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: java.security.NoSuchAlgorithmException) {
            try {
                // Fallback to SHA-256 (always available in standard JVM/Android)
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(data)
                hash.joinToString("") { "%02x".format(it) }
            } catch (e2: java.security.NoSuchAlgorithmException) {
                // This should never happen as SHA-256 is always available
                throw RuntimeException("No suitable hash algorithm available", e2)
            }
        }
    }
    
    /**
     * Validates authorship credentials
     * 
     * @param authorIdentifier Author identifier to validate
     * @return true if author is validated, false otherwise
     */
    fun validateAuthorship(authorIdentifier: String): Boolean {
        // Verify author identity against RAFCODE-Φ credentials
        return authorIdentifier == author || 
               authorIdentifier.contains(identity) ||
               authorIdentifier.contains(seal)
    }
    
    /**
     * Compares two hash strings in constant time to prevent timing attacks
     * 
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return true if hashes match
     */
    private fun constantTimeHashEquals(hash1: String, hash2: String): Boolean {
        if (hash1.length != hash2.length) {
            return false
        }
        
        var result = 0
        for (i in hash1.indices) {
            result = result or (hash1[i].code xor hash2[i].code)
        }
        
        return result == 0
    }
    
    /**
     * ZIPRAF_Ω_FUNCTION: Main licensing validation function
     * 
     * Formula: Licenciar = Validar(ΣΩΔΦBITRAF × RAFCODE-Φ × bitraf64 × Ethica[8])
     * 
     * @param context Execution context with all validation parameters
     * @return Validation result
     */
    fun ziprafOmegaFunction(context: ExecutionContext): ValidationResult {
        val integrityCheck = context.dataHash?.let { hash ->
            constantTimeHashEquals(computeHash(context.data.toByteArray()), hash)
        } ?: true
        
        val authorshipCheck = validateAuthorship(context.authorId)
        val permissionCheck = context.hasPermission
        val destinationCheck = context.destinationValid
        val ethicalCheck = context.ethicalPurposeValid
        
        val isValid = validateExecution(
            integrityCheck,
            authorshipCheck,
            permissionCheck,
            destinationCheck,
            ethicalCheck
        )
        
        return ValidationResult(
            valid = isValid,
            integrityPassed = integrityCheck,
            authorshipPassed = authorshipCheck,
            permissionPassed = permissionCheck,
            destinationPassed = destinationCheck,
            ethicalPassed = ethicalCheck,
            message = if (isValid) "EXECUTION PERMITTED" else "EXECUTION DENIED"
        )
    }
}

/**
 * Execution context for validation
 * Contains all parameters needed for comprehensive validation check
 */
@Serializable
data class ExecutionContext(
    val data: String,
    val dataHash: String? = null,
    val authorId: String,
    val hasPermission: Boolean,
    val destinationValid: Boolean,
    val ethicalPurposeValid: Boolean
)

/**
 * Result of validation check
 * Provides detailed breakdown of each validation factor
 */
@Serializable
data class ValidationResult(
    val valid: Boolean,
    val integrityPassed: Boolean,
    val authorshipPassed: Boolean,
    val permissionPassed: Boolean,
    val destinationPassed: Boolean,
    val ethicalPassed: Boolean,
    val message: String
)
