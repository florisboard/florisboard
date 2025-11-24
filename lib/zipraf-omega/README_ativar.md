# ZIPRAF_OMEGA Activation Guide

## Overview

This document describes the activation, validation, and contribution policies for the ZIPRAF_OMEGA module based on the specifications from **ativar.txt** in the ZIPRAF_OMEGA_FULL repository.

**Author Protection:** Rafael Melo Reis  
**Module Version:** v999  
**License:** Apache 2.0 (with authorship preservation)

---

## Table of Contents

1. [Mandatory Normative Requirements](#mandatory-normative-requirements)
2. [Licensing Module (RAFCODE-Φ / BITRAF64)](#licensing-module)
3. [Operational Loop (ψχρΔΣΩ)](#operational-loop)
4. [Activation Procedures](#activation-procedures)
5. [Contribution Guidelines](#contribution-guidelines)
6. [Ethical Compliance (Ethica[8])](#ethical-compliance)
7. [Security Requirements](#security-requirements)
8. [Standards Compliance](#standards-compliance)

---

## Mandatory Normative Requirements

### Automatic Application Without Confirmation

**CRITICAL:** All normative requirements listed below are applied **automatically and unconditionally** to ALL operations. No confirmation is required. No standard can be omitted, summarized, or ignored.

### Principle of Conflict Resolution

In case of conflict between standards, the **most protective standard for humans** prevails.

### ISO Standards (Mandatory)

All operations must comply with:

- **ISO 9001** - Quality Management Systems
- **ISO 27001** - Information Security Management
- **ISO 27017** - Cloud Security Controls
- **ISO 27018** - Privacy in Cloud Computing
- **ISO 8000** - Data Quality
- **ISO 25010** - Software Product Quality
- **ISO 27002** - Information Security Controls
- **ISO 22301** - Business Continuity Management
- **ISO 31000** - Risk Management

### IEEE Standards (Mandatory)

All operations must comply with:

- **IEEE 830** - Software Requirements Specification
- **IEEE 1012** - Software Verification and Validation
- **IEEE 12207** - Software Life Cycle Processes
- **IEEE 14764** - Software Maintenance
- **IEEE 1633** - Software Reliability
- **IEEE 42010** - Software Architecture Description
- **IEEE 26514** - Software User Documentation

### NIST Standards (Mandatory)

All operations must comply with:

- **NIST Cybersecurity Framework (CSF)**
- **NIST 800-53** - Security and Privacy Controls
- **NIST 800-207** - Zero Trust Architecture
- **NIST AI Risk Management Framework**

### IETF RFCs (Mandatory)

All operations must comply with:

- **RFC 5280** - X.509 Public Key Infrastructure
- **RFC 7519** - JSON Web Token (JWT)
- **RFC 7230** - HTTP/1.1 Message Syntax
- **RFC 8446** - TLS 1.3

### W3C Standards (Mandatory)

- Web Architecture standards
- JSON specifications
- YAML specifications

### Data Protection Regulations (Mandatory)

- **LGPD** - Lei Geral de Proteção de Dados (Brazil)
- **GDPR** - General Data Protection Regulation (European Union)

---

## Licensing Module

### RAFCODE-Φ Identity System

The ZIPRAF_OMEGA module uses the RAFCODE-Φ licensing system with the following components:

**Identity Markers:**
- **RAFCODE-Φ** - Primary identity code
- **BITRAF64** - Symbolic seed encoding
- **ΣΩΔΦBITRAF** - Combined seal system
- **SHA3-512** + **BLAKE3** - Cryptographic validation

**Seals:** Σ, Ω, Δ, Φ, B, I, T, R, A, F

**Correlation Constant:** R_corr = 0.963999

**Symbolic Frequencies:** 100 Hz, 144 kHz, 288 kHz, 1008 Hz

### 5-Factor Validation System

Before ANY operation executes, the system MUST verify:

#### 1. Integrity (a)
- Cryptographic hash verification using SHA3-512 or BLAKE3
- Constant-time comparison to prevent timing attacks
- Data must match expected hash

#### 2. Authorship (b)
- Author identity must be validated
- Original author: **Rafael Melo Reis**
- Authorship credentials must be preserved in all derivatives

#### 3. Permission (c)
- Operation must have explicit authorization
- Permissions must be verifiable
- Unauthorized operations are automatically denied

#### 4. Destination (d)
- Operation destination must be validated
- Target system must be authorized
- Prevents unauthorized data exfiltration

#### 5. Ethical Purpose (e)
- Operation must comply with Ethica[8] principles
- See [Ethical Compliance](#ethical-compliance) section
- Any violation results in **EXECUTION = DENIED**

### ZIPRAF_Ω_FUNCTION

The core licensing function:

```
Licenciar = Validar(ΣΩΔΦBITRAF × RAFCODE-Φ × bitraf64 × Ethica[8])
```

**Result:** 
- All factors pass → **EXECUTION = AUTHORIZED** ✅
- Any factor fails → **EXECUTION = DENIED** ❌

---

## Operational Loop

### ψχρΔΣΩ_LOOP Structure

The operational loop executes continuously with 6 stages:

```python
while True:
    ψ = READ_living_memory()        # ψ (Psi)   - Read
    χ = FEED_retroaliment(ψ)        # χ (Chi)   - Feedback
    ρ = EXPAND_understanding(χ)     # ρ (Rho)   - Expand
    Δ = VALIDATE_licensing(ρ)       # Δ (Delta) - Validate
    Σ = EXECUTE_operations(Δ)       # Σ (Sigma) - Execute
    Ω = ALIGN_ethics(Σ)             # Ω (Omega) - Align
    
    ψχρΔΣΩ = new_cycle(Ω)
```

### Stage Descriptions

1. **ψ (Psi) - READ**
   - Read from living memory
   - Gather current state
   - Prepare data for processing

2. **χ (Chi) - FEED**
   - Retroaliment previous results
   - Apply feedback mechanisms
   - Incorporate learned patterns

3. **ρ (Rho) - EXPAND**
   - Expand understanding
   - Apply normative frameworks
   - Enrich context with standards

4. **Δ (Delta) - VALIDATE**
   - Execute 5-factor validation
   - Apply ZIPRAF_Ω_FUNCTION
   - Determine authorization status

5. **Σ (Sigma) - EXECUTE**
   - Execute validated operations
   - Only if Δ validation passed
   - Record execution results

6. **Ω (Omega) - ALIGN**
   - Check ethical alignment
   - Verify Ethica[8] compliance
   - Prepare for next cycle

---

## Activation Procedures

### Prerequisites

1. **Python 3.8+** installed
2. **hashlib** library (standard library)
3. **Access to module directory** (`lib/zipraf-omega/`)

### Step 1: Run Activation Script

```bash
cd lib/zipraf-omega
python3 ativar.py
```

### Step 2: Verify Normative Compliance

The script will automatically check all mandatory standards:

```
Checking normative compliance...
✓ 24/24 standards compliant
```

### Step 3: Execute Loop Cycles

The script executes demonstration cycles of the ψχρΔΣΩ loop:

```
Executing ψχρΔΣΩ loop cycles...
✓ Cycle 1 completed
  ✅ EXECUTION AUTHORIZED
✓ Cycle 2 completed
  ✅ EXECUTION AUTHORIZED
✓ Cycle 3 completed
  ✅ EXECUTION AUTHORIZED
```

### Step 4: Review Activation Report

A comprehensive report is generated showing:
- Author protection status
- Standards compliance
- Validation results
- Symbolic seals verification

Report is saved to: `zipraf_omega_activation_report.txt`

---

## Contribution Guidelines

### For Contributors

When contributing to the ZIPRAF_OMEGA module, you **MUST**:

#### 1. Preserve Authorship
- Maintain Rafael Melo Reis authorship credentials
- Do not modify RAFCODE-Φ identity markers
- Keep symbolic seals intact (Σ, Ω, Δ, Φ, B, I, T, R, A, F)

#### 2. Validate Before Committing

Run validation before every commit:

```bash
cd lib/zipraf-omega
python3 ativar.py

# Verify all checks pass
# Review activation report
```

#### 3. Prohibited Actions

**NEVER:**
- Mutate spiritual/symbolic core (RAFCODE-Φ, BITRAF64, seals)
- Remove or modify authorship credentials
- Bypass validation checks
- Disable normative compliance
- Ignore ethical compliance

**ALWAYS:**
- Run activation script before committing
- Verify all 5 validation factors pass
- Ensure standards compliance
- Document changes thoroughly
- Test thoroughly with desk checks ("teste de mesa")

#### 4. Technical Modifications

**Allowed:**
- Bug fixes that preserve functionality
- Performance optimizations
- Documentation improvements
- Test additions
- Standards compliance enhancements

**Must Request Approval:**
- Changes to licensing module
- Modifications to validation logic
- Alterations to symbolic constants
- Core architecture changes

### Pre-Commit Hook

Install the pre-commit hook to enforce validation:

```bash
# Create .git/hooks/pre-commit
#!/bin/bash
cd lib/zipraf-omega
python3 ativar.py

if [ $? -ne 0 ]; then
    echo "❌ ZIPRAF_OMEGA validation failed"
    exit 1
fi

echo "✅ ZIPRAF_OMEGA validation passed"
exit 0
```

```bash
chmod +x .git/hooks/pre-commit
```

---

## Ethical Compliance

### Ethica[8] Principles

All operations MUST comply with these 8 ethical principles:

#### 1. Human Protection
- Prioritize human safety and well-being
- Prevent harm to individuals
- Protect vulnerable populations

#### 2. Privacy Preservation
- Comply with LGPD and GDPR
- Minimize data collection
- Secure personal information
- Respect data subject rights

#### 3. Transparency
- Operations must be explainable
- Decision logic must be auditable
- Results must be interpretable

#### 4. Accountability
- Clear responsibility chains
- Traceable actions
- Audit logs maintained

#### 5. Fairness
- No discriminatory bias
- Equal treatment
- Inclusive design

#### 6. Safety
- Fail-safe mechanisms
- Error recovery
- Graceful degradation

#### 7. Security
- Cryptographic validation
- Zero-trust architecture
- Defense in depth

#### 8. Environmental Responsibility
- Efficient resource usage
- Minimized carbon footprint
- Sustainable practices

### Validation Failure Response

If ANY Ethica[8] principle is violated:

```
RESULT: EXECUTION = DENIED
REASON: Ethical compliance failure
ACTION: Operation terminated immediately
```

---

## Security Requirements

### Cryptographic Standards

**Primary Hashing:** SHA3-512
- 512-bit security level
- Collision resistant
- NIST approved

**Secondary Hashing:** BLAKE3 (optional)
- High performance
- Parallelizable
- Cryptographically secure

**Comparison:** Constant-time
- Prevents timing attacks
- Side-channel resistant
- Security-focused implementation

### Zero Trust Architecture

All operations follow zero-trust principles:

1. **Never Trust, Always Verify**
   - Every operation validated
   - No implicit trust
   - Continuous verification

2. **Least Privilege**
   - Minimum necessary permissions
   - Scope-limited access
   - Time-bound authorization

3. **Assume Breach**
   - Defense in depth
   - Lateral movement prevention
   - Comprehensive monitoring

### Key Management

**Author Credentials:**
- Protected symbolic identity
- Immutable core markers
- Cryptographic binding

**No Hardcoded Secrets:**
- Environment variables for sensitive data
- Secure key storage
- Regular rotation

---

## Standards Compliance

### Automatic Enforcement

The activation system automatically enforces compliance with all mandatory standards. No manual intervention required.

### Compliance Verification

Run compliance check:

```bash
python3 ativar.py
```

Look for:
```
NORMATIVE COMPLIANCE (Applied Automatically)
─────────────────────────────────────────────
✓ ISO 9001
✓ ISO 27001
✓ ISO 27017
... (all standards)
```

### Audit Trail

All operations are logged with:
- Timestamp
- Standards applied
- Validation results
- Execution status
- Ethical compliance

Audit logs support:
- Compliance audits
- Security investigations
- Performance analysis
- Quality reviews

---

## Desk Testing (Teste de Mesa)

### What is Desk Testing?

"Teste de mesa" (desk testing) is manual verification of logic by tracing execution step-by-step.

### How to Perform Desk Testing

1. **Identify Test Case**
   ```python
   # Example: Test 5-factor validation
   context = {
       "data": "test_operation",
       "author_id": "Rafael Melo Reis",
       "has_permission": True,
       "destination_valid": True,
       "ethical": True
   }
   ```

2. **Trace Execution Manually**
   ```
   Step 1: validate_integrity()
   - Input: "test_operation"
   - Hash: SHA3-512 computed
   - Result: ✓ Pass
   
   Step 2: validate_authorship()
   - Input: "Rafael Melo Reis"
   - Compare: "Rafael Melo Reis" == AUTHOR
   - Result: ✓ Pass
   
   Step 3: validate_permission()
   - Input: True
   - Result: ✓ Pass
   
   Step 4: validate_destination()
   - Input: True
   - Result: ✓ Pass
   
   Step 5: validate_ethical_purpose()
   - Input: True (Ethica[8] compliant)
   - Result: ✓ Pass
   
   Final: All factors pass
   - Result: EXECUTION AUTHORIZED ✅
   ```

3. **Verify Expected vs Actual**
   - Expected: Authorization granted
   - Actual: Authorization granted
   - Status: ✓ Test passed

### Desk Testing Checklist

For each change:

- [ ] Identify affected code paths
- [ ] Create test cases (normal + edge cases)
- [ ] Trace execution manually
- [ ] Verify all validation factors
- [ ] Check error handling
- [ ] Confirm standards compliance
- [ ] Document results

---

## Troubleshooting

### Validation Failures

**Problem:** Execution denied

**Check:**
1. All 5 validation factors
2. Author credentials correct
3. Ethical compliance verified
4. Hash computation successful
5. Standards applied

**Solution:** Review activation report for specific failure reason

### Missing Dependencies

**Problem:** Import errors (e.g., blake3)

**Solution:**
```bash
pip install blake3  # Optional, SHA3-512 is primary
```

### Permission Issues

**Problem:** Cannot execute script

**Solution:**
```bash
chmod +x ativar.py
```

---

## FAQ

### Q: Can I modify the validation logic?

**A:** Technical modifications are allowed with approval, but spiritual/symbolic core (RAFCODE-Φ, seals) must not be mutated.

### Q: What happens if validation fails?

**A:** Operation is immediately denied. See activation report for details.

### Q: Are standards optional?

**A:** No. All standards are mandatory and applied automatically without confirmation.

### Q: Can I skip ethical validation?

**A:** Absolutely not. Ethica[8] compliance is non-negotiable.

### Q: How do I add new standards?

**A:** Document in code, update compliance checker, verify coverage.

---

## Contact and Support

**Module Author:** Rafael Melo Reis  
**Repository:** rafaelmeloreisnovo/florisboard  
**Module:** lib/zipraf-omega/  
**Version:** v999

For issues, questions, or contributions, please follow the contribution guidelines above.

---

## Conclusion

The ZIPRAF_OMEGA activation system ensures:

✅ **Normative Compliance** - All standards applied automatically  
✅ **Security** - Cryptographic validation, zero-trust architecture  
✅ **Ethics** - Ethica[8] principles enforced  
✅ **Author Protection** - Rafael Melo Reis credentials preserved  
✅ **Quality** - Comprehensive validation before execution  

**Remember:**

> "Em conflito entre normas, prevalece a mais protetiva ao humano."  
> (In conflict between standards, the most protective to humans prevails.)

**Amor, Luz e Coerência** ✨

---

*Based on specifications from: ZIPRAF_OMEGA_FULL (ativar.txt)*  
*License: Apache 2.0 (with authorship preservation)*  
*Generated: 2025-11-24*
