/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package org.openj9.envInfo;

public class JavaInfo {

    public String getSPEC(String javaImplInfo) {
        String osName = System.getProperty("os.name").toLowerCase();
        System.out.println("System.getProperty('os.name')=" + System.getProperty("os.name") + "\n");
        String osArch = System.getProperty("os.arch").toLowerCase();
        System.out.println("System.getProperty('os.arch')=" + System.getProperty("os.arch") + "\n");
        String fullversion = System.getProperty("java.fullversion");
        System.out.println("System.getProperty('java.fullversion')=" + fullversion + "\n");
        String spec = "";
        if (osName.contains("linux")) {
            spec = "linux";
        } else if (osName.contains("win")) {
            spec = "win";
        } else if (osName.contains("mac")) {
            spec = "osx";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            spec = "aix";
        } else if (osName.contains("z/os")) {
            spec = "zos";
        } else if (osName.contains("sunos")) {
            spec = "sunos";
        } else if (osName.contains("bsd")) {
            spec = "bsd";
        } else {
            System.out.println("Cannot determine System.getProperty('os.name')=" + osName + "\n");
            return null;
        }

        if (osArch.contains("amd64") || osArch.contains("x86")) {
            spec += "_x86";
        } else if (osArch.contains("ppc") || osArch.contains("powerpc")) {
            spec += "_ppc";
        } else if (osArch.contains("s390")) {
            spec += "_390";
        } else if (osArch.contains("aarch") || osArch.contains("arm")) {
            spec += "_arm";
        } else if (osArch.contains("sparcv9")) {
            spec += "_sparcv9";
        } else if (osArch.contains("riscv")) {
            spec += "_riscv";
        } else {
            System.out.println("Cannot determine System.getProperty('os.arch')=" + osArch + "\n");
            return null;
        }

        String model = System.getProperty("sun.arch.data.model");
        System.out.println("System.getProperty('sun.arch.data.model')=" + model + "\n");
        if (Integer.parseInt(model.trim())==64) {
            spec += "-64";
            spec = spec.replace("arm-64", "aarch64");
            spec = spec.replace("riscv-64", "riscv64");
        }

        if (javaImplInfo.equals("ibm") || javaImplInfo.equals("openj9")) {
            spec += cmprssptrs();
        }

        if (spec.contains("ppc") && osArch.contains("le")) {
            spec += "_le";
        }
        
        return spec;
    }

    private String cmprssptrs() {
        String rt = "";
        CmdExecutor ce = CmdExecutor.getInstance();
        String exe = System.getProperty("java.home") + "/bin/java";
        String ver = "-version";
        String comp = ce.execute(new String[] {exe, "-Xcompressedrefs", ver});
        String nocomp = ce.execute(new String[] {exe, "-Xnocompressedrefs", ver});
        if (comp.contains(System.getProperty("java.version"))) {
            rt = "_cmprssptrs";
            if (nocomp.contains(System.getProperty("java.version"))) {
                rt = "_mxdptrs";
            }
        }
        return rt;
    }

    public int getJDKVersion() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.")) {
            javaVersion = javaVersion.substring(2);
        }
        int dotIndex = javaVersion.indexOf('.');
        int dashIndex = javaVersion.indexOf('-');
        try {
            return Integer.parseInt(javaVersion.substring(0, dotIndex > -1 ? dotIndex : dashIndex > -1 ? dashIndex : javaVersion.length()));
        } catch (NumberFormatException e) {
            System.out.println("Cannot determine System.getProperty('java.version')=" + javaVersion + "\n");
            return -1;
        }
    }


    public String getJDKImpl() {
        String impl = System.getProperty("java.vm.name");
        System.out.println("System.getProperty('java.vm.name')=" + impl + "\n");
        impl = impl.toLowerCase();
        if (impl.contains("ibm")) {
            return "ibm";
        } else if (impl.contains("openj9")) {
            return "openj9";
        } else if (impl.contains("oracle") || impl.contains("hotspot") || impl.contains("openjdk")) {
            return "hotspot";
        } else {
            System.out.println("Cannot determine System.getProperty('java.vm.name')=" + impl + "\n");
            return null;
        }
    }

    public String getJDKVendor() {
        String vendor = System.getProperty("java.vendor");
        System.out.println("System.getProperty('java.vendor')=" + vendor + "\n");
        String vendorLC = vendor.toLowerCase();
        if (vendorLC.contains("adoptopenjdk")) {
            return "adoptopenjdk";
        } else if (vendorLC.contains("eclipse")) {
            return "eclipse";
        } else if (vendorLC.contains("ibm")) {
            return "ibm";
        } else if (vendorLC.contains("alibaba")) {
            return "alibaba";
        } else if (vendorLC.contains("amazon")) {
            return "amazon";
        } else if (vendorLC.contains("azul")) {
            return "azul";
        }  else if (vendorLC.contains("sap")) {
            return "sap";
        } else if (vendorLC.contains("bellsoft")) {
            return "bellsoft";
        } else if (vendorLC.contains("oracle")) {
            return "oracle";
        } else {
            System.out.println("Warning: cannot determine vendor, use System.getProperty('java.vendor')=" + vendor + " directly.\n");
            return vendor;
        }
    }

}
