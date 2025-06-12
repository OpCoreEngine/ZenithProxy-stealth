import os
import platform
import subprocess
import sys
from enum import Enum

from jdk_install import get_java_executable


def validate_linux_cpu_flags():
    x86_64_v3_flags = ["avx", "avx2", "bmi1", "bmi2", "fma", "sse4_1", "sse4_2", "ssse3"]
    try:
        output = subprocess.check_output(["cat", "/proc/cpuinfo"], stderr=subprocess.STDOUT, text=True)
        flags = []
        for line in output.splitlines():
            if line.startswith("flags"):
                flags = line.split(": ")[1].split(" ")
                break
        for flag in x86_64_v3_flags:
            if flag not in flags:
                print(
                    "Unsupported CPU. "
                    + "Use the Java release channel instead. Re-run setup to change the release channel. "
                    + "\nFlag not found: "
                    + flag
                )
                return False
        return True
    except Exception as e:
        print("Error checking CPU flags:", e)
        return False


def validate_linux_glibc_version(config):
    try:
        mc_version = config.get_mc_version()
        old_versions = ["1.12.2", "1.20.1", "1.20.4", "1.20.6", "1.21.0"]
        if mc_version in old_versions:
            glibc_minor_version_min = 31
        else:
            glibc_minor_version_min = 39
        output = subprocess.check_output(["ldd", "--version"], stderr=subprocess.STDOUT, text=True)
        # ldd (Ubuntu GLIBC 2.35-0ubuntu3.4) 2.35
        # get the version from the last word of the first line
        version = output.splitlines()[0].split(" ")[-1]
        version = version.split(".")
        if int(version[0]) != 2 or int(version[1]) < glibc_minor_version_min:
            print(
                "Unsupported OS for linux release channel.\nglibc version too low: "
                + str(version)
                + "\nMin glibc version needed: 2."
                + str(glibc_minor_version_min)
            )
            return False
        return True
    except Exception as e:
        print("Error checking GLIBC version.")
        return False


def validate_linux_system(config):
    try:
        return (
            get_platform_os() == OperatingSystem.LINUX
            and get_platform_arch() == CpuArch.AMD64
            and validate_linux_cpu_flags()
            and validate_linux_glibc_version(config)
        )
    except Exception:
        return False


def validate_java_system(config):
    min_java_version = 17 if config.version.startswith("1") else 21
    java_executable = get_java_executable()
    if java_executable is None:
        print(f"Java >={min_java_version} not found.")
        return False
    return True


def validate_git_system():
    return os.path.isdir(".git")


def validate_system_with_config(config):
    if config.release_channel == "git":
        return validate_git_system()
    elif config.release_channel.startswith("java"):
        return validate_java_system(config)
    elif config.release_channel.startswith("linux"):
        return validate_linux_system(config)
    else:
        return False


def is_pyinstaller_bundle():
    return getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS")


def is_nuitka_bundle():
    return "__compiled__" in globals()


def is_windows_python_bundle():
    return os.path.exists("python/python.exe")


def executable_path():
    if is_nuitka_bundle():
        return sys.argv[0]
    return sys.executable


class PlatformError(Exception):
    pass


class OperatingSystem(Enum):
    WINDOWS = "windows"
    LINUX = "linux"
    MACOS = "macos"


def get_platform_os():
    if platform.system() == "Windows":
        return OperatingSystem.WINDOWS
    elif platform.system() == "Linux":
        return OperatingSystem.LINUX
    elif platform.system() == "Darwin":
        return OperatingSystem.MACOS
    else:
        raise PlatformError("Unsupported OS: " + platform.system())


class CpuArch(Enum):
    AMD64 = "amd64"
    AARCH64 = "aarch64"


def get_platform_arch():
    uname = platform.machine().lower()
    arm64_names = ["aarch64", "arm64", "aarch64_be", "armv8b", "armv8l"]
    x64_names = ["amd64", "x86_64", "x64"]
    if uname in arm64_names:
        return CpuArch.AARCH64
    elif uname in x64_names:
        return CpuArch.AMD64
    else:
        raise PlatformError("Unsupported CPU architecture: " + uname)
