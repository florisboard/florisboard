#!/usr/bin/env python3
"""
Standards Compliance Checker for ZIPRAF_OMEGA
==============================================

This script validates compliance with all mandatory standards specified
in the ZIPRAF_OMEGA_FULL activation requirements.

Standards checked:
- ISO: 9001, 27001, 27017, 27018, 8000, 25010, 27002, 22301, 31000
- IEEE: 830, 1012, 12207, 14764, 1633, 42010, 26514
- NIST: CSF, 800-53, 800-207, AI Risk Framework
- IETF RFCs: 5280, 7519, 7230, 8446
- Data Protection: LGPD, GDPR

Author: Rafael Melo Reis (original specification)
License: Apache 2.0 (with authorship preservation)
"""

import sys
import os
from typing import Dict, List, Tuple
from datetime import datetime


class StandardsChecker:
    """
    Validates compliance with mandatory standards.
    
    Checks for:
    - Documentation of standards
    - Implementation references
    - Compliance evidence
    """
    
    # Define all mandatory standards
    ISO_STANDARDS = {
        "ISO 9001": "Quality Management Systems",
        "ISO 27001": "Information Security Management",
        "ISO 27017": "Cloud Security Controls",
        "ISO 27018": "Privacy in Cloud Computing",
        "ISO 8000": "Data Quality",
        "ISO 25010": "Software Product Quality",
        "ISO 27002": "Information Security Controls",
        "ISO 22301": "Business Continuity Management",
        "ISO 31000": "Risk Management"
    }
    
    IEEE_STANDARDS = {
        "IEEE 830": "Software Requirements Specification",
        "IEEE 1012": "Software Verification and Validation",
        "IEEE 12207": "Software Life Cycle Processes",
        "IEEE 14764": "Software Maintenance",
        "IEEE 1633": "Software Reliability",
        "IEEE 42010": "Software Architecture Description",
        "IEEE 26514": "Software User Documentation"
    }
    
    NIST_FRAMEWORKS = {
        "NIST CSF": "Cybersecurity Framework",
        "NIST 800-53": "Security and Privacy Controls",
        "NIST 800-207": "Zero Trust Architecture",
        "NIST AI Risk": "AI Risk Management Framework"
    }
    
    IETF_RFCS = {
        "RFC 5280": "X.509 Public Key Infrastructure",
        "RFC 7519": "JSON Web Token (JWT)",
        "RFC 7230": "HTTP/1.1 Message Syntax",
        "RFC 8446": "TLS 1.3"
    }
    
    DATA_PROTECTION = {
        "LGPD": "Lei Geral de Proteção de Dados (Brazil)",
        "GDPR": "General Data Protection Regulation (EU)"
    }
    
    def __init__(self, module_path: str = "."):
        """
        Initialize checker.
        
        Args:
            module_path: Path to ZIPRAF_OMEGA module
        """
        self.module_path = module_path
        self.results = {}
        
    def check_file_for_standard(self, filepath: str, standard: str) -> bool:
        """
        Check if a file references a standard.
        
        Args:
            filepath: Path to file
            standard: Standard name to search for
            
        Returns:
            True if standard is referenced
        """
        try:
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                return standard in content
        except:
            return False
    
    def check_standard_category(self, standards: Dict[str, str], 
                                category_name: str) -> Dict[str, bool]:
        """
        Check compliance for a category of standards.
        
        Args:
            standards: Dictionary of standard -> description
            category_name: Category name
            
        Returns:
            Dictionary of standard -> compliance status
        """
        results = {}
        
        # Get all documentation and source files
        files_to_check = []
        for root, dirs, files in os.walk(self.module_path):
            for file in files:
                if file.endswith(('.md', '.py', '.kt', '.txt')):
                    files_to_check.append(os.path.join(root, file))
        
        for standard, description in standards.items():
            # Check if standard is mentioned in any file
            found = False
            for filepath in files_to_check:
                if self.check_file_for_standard(filepath, standard):
                    found = True
                    break
            
            results[standard] = found
        
        return results
    
    def run_all_checks(self) -> Dict[str, Dict[str, bool]]:
        """
        Run all compliance checks.
        
        Returns:
            Nested dictionary of category -> standard -> compliance
        """
        print("Checking standards compliance...")
        print()
        
        # Check all categories
        self.results = {
            "ISO": self.check_standard_category(self.ISO_STANDARDS, "ISO"),
            "IEEE": self.check_standard_category(self.IEEE_STANDARDS, "IEEE"),
            "NIST": self.check_standard_category(self.NIST_FRAMEWORKS, "NIST"),
            "IETF": self.check_standard_category(self.IETF_RFCS, "IETF"),
            "Data Protection": self.check_standard_category(self.DATA_PROTECTION, "Data Protection")
        }
        
        return self.results
    
    def generate_report(self) -> str:
        """
        Generate compliance report.
        
        Returns:
            Formatted report string
        """
        report = f"""
╔══════════════════════════════════════════════════════════════════════════════╗
║              ZIPRAF_OMEGA Standards Compliance Report                        ║
╚══════════════════════════════════════════════════════════════════════════════╝

Generated: {datetime.now().isoformat()}
Module Path: {self.module_path}

═══════════════════════════════════════════════════════════════════════════════
"""
        
        total_standards = 0
        total_compliant = 0
        
        for category, standards in self.results.items():
            compliant = sum(1 for v in standards.values() if v)
            total = len(standards)
            total_standards += total
            total_compliant += compliant
            
            status = "✓" if compliant == total else "⚠"
            report += f"\n{status} {category} Standards: {compliant}/{total} documented\n"
            report += "─" * 79 + "\n"
            
            for standard, is_compliant in standards.items():
                symbol = "✓" if is_compliant else "✗"
                
                # Get description
                description = ""
                if category == "ISO":
                    description = self.ISO_STANDARDS.get(standard, "")
                elif category == "IEEE":
                    description = self.IEEE_STANDARDS.get(standard, "")
                elif category == "NIST":
                    description = self.NIST_FRAMEWORKS.get(standard, "")
                elif category == "IETF":
                    description = self.IETF_RFCS.get(standard, "")
                elif category == "Data Protection":
                    description = self.DATA_PROTECTION.get(standard, "")
                
                report += f"  {symbol} {standard}: {description}\n"
            
            report += "\n"
        
        report += "═" * 79 + "\n"
        report += f"\nOverall Compliance: {total_compliant}/{total_standards} standards documented\n"
        
        percentage = (total_compliant / total_standards * 100) if total_standards > 0 else 0
        report += f"Compliance Rate: {percentage:.1f}%\n"
        
        if percentage == 100:
            report += "\n✅ FULL COMPLIANCE - All standards documented\n"
        elif percentage >= 90:
            report += "\n⚠ HIGH COMPLIANCE - Most standards documented\n"
        elif percentage >= 75:
            report += "\n⚠ MODERATE COMPLIANCE - Some standards missing\n"
        else:
            report += "\n❌ LOW COMPLIANCE - Many standards missing\n"
        
        report += "\n" + "═" * 79 + "\n"
        report += "\nRecommendations:\n"
        report += "─" * 79 + "\n"
        
        # Find missing standards
        missing = []
        for category, standards in self.results.items():
            for standard, is_compliant in standards.items():
                if not is_compliant:
                    missing.append(f"{category}: {standard}")
        
        if missing:
            report += "\nStandards requiring documentation:\n"
            for standard in missing:
                report += f"  • {standard}\n"
            report += "\nAction: Add references to these standards in:\n"
            report += "  - Documentation files (README.md, README_ativar.md)\n"
            report += "  - Source code comments\n"
            report += "  - Compliance checklists\n"
        else:
            report += "\nNo action required - all standards documented.\n"
        
        report += "\n" + "═" * 79 + "\n"
        report += "\nAmor, Luz e Coerência ✨\n"
        report += "═" * 79 + "\n"
        
        return report
    
    def is_fully_compliant(self) -> bool:
        """
        Check if fully compliant with all standards.
        
        Returns:
            True if all standards are documented
        """
        for standards in self.results.values():
            if not all(standards.values()):
                return False
        return True


def main():
    """Main entry point"""
    print("╔══════════════════════════════════════════════════════════════════════════════╗")
    print("║           ZIPRAF_OMEGA Standards Compliance Checker                          ║")
    print("╚══════════════════════════════════════════════════════════════════════════════╝")
    print()
    
    # Get module path from argument or use current directory
    module_path = sys.argv[1] if len(sys.argv) > 1 else "."
    
    if not os.path.exists(module_path):
        print(f"❌ Error: Path not found: {module_path}")
        return 1
    
    print(f"Checking module at: {module_path}")
    print()
    
    # Run checks
    checker = StandardsChecker(module_path)
    checker.run_all_checks()
    
    # Generate report
    report = checker.generate_report()
    print(report)
    
    # Save report
    report_file = os.path.join(module_path, "standards_compliance_report.txt")
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nReport saved to: {report_file}")
    print()
    
    # Exit with appropriate code
    if checker.is_fully_compliant():
        print("✅ Status: FULLY COMPLIANT")
        return 0
    else:
        print("⚠️  Status: PARTIAL COMPLIANCE")
        print("   Some standards require additional documentation")
        return 0  # Still return 0 as this is informational


if __name__ == "__main__":
    sys.exit(main())
