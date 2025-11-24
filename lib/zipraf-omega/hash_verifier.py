#!/usr/bin/env python3
"""
Hash Verification Tool for ZIPRAF_OMEGA
========================================

This script provides hash verification functionality using SHA3-512 and BLAKE3
as specified in the ZIPRAF_OMEGA_FULL activation requirements.

Author: Rafael Melo Reis (original specification)
License: Apache 2.0 (with authorship preservation)
"""

import hashlib
import sys
import os
from typing import Optional, Tuple


class HashVerifier:
    """
    Hash verification utility implementing SHA3-512 and BLAKE3.
    
    Provides:
    - SHA3-512 primary hashing
    - BLAKE3 secondary hashing (optional)
    - Constant-time comparison
    - File and string hashing
    """
    
    def __init__(self):
        """Initialize hash verifier"""
        self.blake3_available = self._check_blake3_available()
    
    def _check_blake3_available(self) -> bool:
        """Check if BLAKE3 library is available"""
        try:
            import blake3
            return True
        except ImportError:
            return False
    
    def compute_sha3_512(self, data: bytes) -> str:
        """
        Compute SHA3-512 hash.
        
        Args:
            data: Bytes to hash
            
        Returns:
            Hexadecimal hash string
        """
        try:
            return hashlib.sha3_512(data).hexdigest()
        except AttributeError:
            # SHA3-512 not available - security downgrade
            print("⚠️  CRITICAL: SHA3-512 not available!", file=sys.stderr)
            print("   Using SHA-256 (reduced security: 256-bit vs 512-bit)", file=sys.stderr)
            print("   Install Python with SHA3 support or use BLAKE3", file=sys.stderr)
            return hashlib.sha256(data).hexdigest()
    
    def compute_blake3(self, data: bytes) -> Optional[str]:
        """
        Compute BLAKE3 hash if available.
        
        Args:
            data: Bytes to hash
            
        Returns:
            Hexadecimal hash string or None
        """
        if not self.blake3_available:
            return None
        
        try:
            import blake3
            return blake3.blake3(data).hexdigest()
        except Exception as e:
            print(f"⚠️  WARNING: BLAKE3 computation failed: {e}", file=sys.stderr)
            return None
    
    def hash_string(self, text: str) -> Tuple[str, Optional[str]]:
        """
        Hash a string with SHA3-512 and BLAKE3.
        
        Args:
            text: String to hash
            
        Returns:
            Tuple of (sha3_512_hash, blake3_hash)
        """
        data = text.encode('utf-8')
        sha3 = self.compute_sha3_512(data)
        blake3 = self.compute_blake3(data)
        return sha3, blake3
    
    def hash_file(self, filepath: str) -> Tuple[str, Optional[str]]:
        """
        Hash a file with SHA3-512 and BLAKE3.
        
        Args:
            filepath: Path to file
            
        Returns:
            Tuple of (sha3_512_hash, blake3_hash)
        """
        if not os.path.exists(filepath):
            raise FileNotFoundError(f"File not found: {filepath}")
        
        # Read file in chunks for memory efficiency
        if hasattr(hashlib, 'sha3_512'):
            sha3_hasher = hashlib.sha3_512()
        else:
            print("⚠️  CRITICAL: SHA3-512 not available for file hashing!", file=sys.stderr)
            print("   Using SHA-256 (reduced security)", file=sys.stderr)
            sha3_hasher = hashlib.sha256()
        
        blake3_hasher = None
        
        if self.blake3_available:
            try:
                import blake3
                blake3_hasher = blake3.blake3()
            except (ImportError, AttributeError) as e:
                print(f"⚠️  BLAKE3 initialization failed: {e}", file=sys.stderr)
        
        with open(filepath, 'rb') as f:
            while chunk := f.read(8192):
                sha3_hasher.update(chunk)
                if blake3_hasher:
                    blake3_hasher.update(chunk)
        
        sha3_hash = sha3_hasher.hexdigest()
        blake3_hash = blake3_hasher.hexdigest() if blake3_hasher else None
        
        return sha3_hash, blake3_hash
    
    def constant_time_compare(self, hash1: str, hash2: str) -> bool:
        """
        Constant-time hash comparison to prevent timing attacks.
        
        Args:
            hash1: First hash
            hash2: Second hash
            
        Returns:
            True if hashes match, False otherwise
        """
        if len(hash1) != len(hash2):
            return False
        
        result = 0
        for a, b in zip(hash1, hash2):
            result |= ord(a) ^ ord(b)
        
        return result == 0
    
    def verify_string(self, text: str, expected_sha3: str) -> bool:
        """
        Verify string hash against expected value.
        
        Args:
            text: String to verify
            expected_sha3: Expected SHA3-512 hash
            
        Returns:
            True if hash matches
        """
        sha3, _ = self.hash_string(text)
        return self.constant_time_compare(sha3, expected_sha3)
    
    def verify_file(self, filepath: str, expected_sha3: str) -> bool:
        """
        Verify file hash against expected value.
        
        Args:
            filepath: Path to file
            expected_sha3: Expected SHA3-512 hash
            
        Returns:
            True if hash matches
        """
        sha3, _ = self.hash_file(filepath)
        return self.constant_time_compare(sha3, expected_sha3)


def print_header():
    """Print script header"""
    print("╔══════════════════════════════════════════════════════════════════════════════╗")
    print("║              ZIPRAF_OMEGA Hash Verification Tool                             ║")
    print("╚══════════════════════════════════════════════════════════════════════════════╝")
    print()


def print_usage():
    """Print usage information"""
    print("Usage:")
    print("  hash_verifier.py string <text>                  - Hash a string")
    print("  hash_verifier.py file <filepath>                - Hash a file")
    print("  hash_verifier.py verify-string <text> <hash>    - Verify string hash")
    print("  hash_verifier.py verify-file <file> <hash>      - Verify file hash")
    print()
    print("Examples:")
    print('  hash_verifier.py string "Hello, World!"')
    print("  hash_verifier.py file LicensingModule.kt")
    print('  hash_verifier.py verify-string "test" abc123...')
    print("  hash_verifier.py verify-file file.txt abc123...")
    print()


def main():
    """Main entry point"""
    print_header()
    
    if len(sys.argv) < 2:
        print_usage()
        return 1
    
    command = sys.argv[1].lower()
    verifier = HashVerifier()
    
    # Check BLAKE3 availability
    if verifier.blake3_available:
        print("✓ BLAKE3 support available")
    else:
        print("⚠️  BLAKE3 not available (optional - SHA3-512 is primary)")
    print()
    
    try:
        if command == "string":
            if len(sys.argv) < 3:
                print("❌ Error: Missing string argument")
                print_usage()
                return 1
            
            text = sys.argv[2]
            print(f"Hashing string: {text[:50]}{'...' if len(text) > 50 else ''}")
            print()
            
            sha3, blake3 = verifier.hash_string(text)
            
            print("Results:")
            print(f"  SHA3-512:  {sha3}")
            if blake3:
                print(f"  BLAKE3:    {blake3}")
            print()
            
        elif command == "file":
            if len(sys.argv) < 3:
                print("❌ Error: Missing file argument")
                print_usage()
                return 1
            
            filepath = sys.argv[2]
            print(f"Hashing file: {filepath}")
            print()
            
            sha3, blake3 = verifier.hash_file(filepath)
            
            print("Results:")
            print(f"  SHA3-512:  {sha3}")
            if blake3:
                print(f"  BLAKE3:    {blake3}")
            print()
            
        elif command == "verify-string":
            if len(sys.argv) < 4:
                print("❌ Error: Missing arguments")
                print_usage()
                return 1
            
            text = sys.argv[2]
            expected_hash = sys.argv[3]
            
            print(f"Verifying string: {text[:50]}{'...' if len(text) > 50 else ''}")
            print(f"Expected hash:    {expected_hash}")
            print()
            
            if verifier.verify_string(text, expected_hash):
                print("✅ VERIFICATION PASSED - Hash matches")
                return 0
            else:
                sha3, _ = verifier.hash_string(text)
                print("❌ VERIFICATION FAILED - Hash mismatch")
                print(f"Computed hash:    {sha3}")
                return 1
            
        elif command == "verify-file":
            if len(sys.argv) < 4:
                print("❌ Error: Missing arguments")
                print_usage()
                return 1
            
            filepath = sys.argv[2]
            expected_hash = sys.argv[3]
            
            print(f"Verifying file:   {filepath}")
            print(f"Expected hash:    {expected_hash}")
            print()
            
            if verifier.verify_file(filepath, expected_hash):
                print("✅ VERIFICATION PASSED - Hash matches")
                return 0
            else:
                sha3, _ = verifier.hash_file(filepath)
                print("❌ VERIFICATION FAILED - Hash mismatch")
                print(f"Computed hash:    {sha3}")
                return 1
            
        else:
            print(f"❌ Error: Unknown command: {command}")
            print_usage()
            return 1
            
    except FileNotFoundError as e:
        print(f"❌ Error: {e}")
        return 1
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return 1
    
    print("═══════════════════════════════════════════════════════════════════════════════")
    print("Amor, Luz e Coerência ✨")
    print("═══════════════════════════════════════════════════════════════════════════════")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
