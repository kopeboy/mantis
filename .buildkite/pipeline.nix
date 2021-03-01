{ cfg, pkgs, ... }:

with cfg.steps.commands;
let
  commonAttrs = {
    retry.automatic = true;
    agents.queue = "project42";
  };
in
{
  steps.commands = {
    nixExpr = commonAttrs // {
      label = "ensure Nix expressions are up-to-date";
      command = ''
        ./update-nix.sh --check
      '';
      retry.automatic = false;
      artifactPaths = [
        "nix-expr.patch"
      ];
    };

    scalafmt = commonAttrs // {
      label = "scalafmtCheck";
      command = ''
        nix-shell --run '$SBT scalafmtCheck'
      '';
    };

    compile = commonAttrs // {
      label = "compile everything";
      dependsOn = [ scalafmt ];
      command = ''
        nix-shell --run '$SBT compile-all'
      '';
    };

    style = commonAttrs // {
      dependsOn = [ compile ];
      label = "scalastyle";
      command = ''
        nix-shell --run '$SBT scalastyle test:scalastyle'
      '';
    };

    test-bytes = commonAttrs // {
      dependsOn = [ compile ];
      label = "bytes tests";
      command = ''
        nix-shell --run '$SBT coverage bytes/test'
      '';
      artifactPaths = [
        "bytes/target/test-reports/**/*"
        "bytes/target/scala-2.13/scoverage-report/**/*"
        "bytes/target/scala-2.13/coverage-report/**/*"
      ];
    };

    test-crypto = commonAttrs // {
      dependsOn = [ compile ];
      label = "Crypto tests";
      command = ''
        nix-shell --run '$SBT coverage crypto/test'
      '';
      artifactPaths = [
        "crypto/target/test-reports/**/*"
        "crypto/target/scala-2.13/scoverage-report/**/*"
        "crypto/target/scala-2.13/coverage-report/**/*"
      ];
    };

    test-rlp = commonAttrs // {
      dependsOn = [ compile ];
      label = "RLP tests";
      command = ''
        nix-shell --run '$SBT coverage rlp/test'
      '';
      artifactPaths = [
        "rlp/target/test-reports/**/*"
        "rlp/target/scala-2.13/scoverage-report/**/*"
        "rlp/target/scala-2.13/coverage-report/**/*"
      ];
    };

    test-unit = commonAttrs // {
      dependsOn = [ compile ];
      label = "unit tests";
      command = ''
        nix-shell --run '$SBT coverage test'
      '';
      artifactPaths = [
        "target/test-reports/**/*"
        "target/scala-2.13/scoverage-report/**/*"
        "target/scala-2.13/coverage-report/**/*"
      ];
    };

    annotate-test-reports = commonAttrs // {
      dependsOn = [ test-unit ];
      label = "annotate test reports";
      command = "junit-annotate";
      allowDependencyFailure = true;
      plugins = [{
        "junit-annotate#1.9.0" = {
          artifacts = "target/test-reports/*.xml";
          report-slowest = 50;
        };
      }];
    };

    test-evm = commonAttrs // {
      dependsOn = [ compile ];
      label = "EVM tests";
      command = ''
        nix-shell --run '$SBT coverage evm:test'
      '';
      artifactPaths = [
        "target/test-reports/**/*"
        "target/scala-2.13/scoverage-report/**/*"
        "target/scala-2.13/coverage-report/**/*"
      ];
    };

    test-integration = commonAttrs // {
      dependsOn = [ compile ];
      label = "integration tests";
      command = ''
        nix-shell --run '$SBT coverageOff it:test'
      '';
      artifactPaths = [ "target/test-reports/**/*" ];
    };

    test-ets = commonAttrs // {
      dependsOn = [ compile ];
      label = "ETS";
      command = ''
        nix-shell --run './test-ets.sh'
      '';
    };

    coverageReport = commonAttrs // {
      dependsOn = [ test-unit test-evm ];
      label = "coverage report";
      command = ''
        nix-shell --run '$SBT coverageReport coverageAggregate'
      '';
    };

    additional = commonAttrs // {
      dependsOn = [ compile ];
      label = "additional compilation & dist";
      command = ''
        nix-shell --run '$SBT benchmark:compile dist'
      '';
    };

    install-gpg = commonAttrs // {
      label = "Install GPG";
      command = ''
        which gpg; yum install -y gnupg2 && mv /usr/bin/gpg /usr/bin/gpg-vanilla && echo '#!/bin/sh\n\n/usr/bin/gpg-vanilla --no-tty --pinentry loopback \$@' > /usr/bin/gpg && chmod 755 /usr/bin/gpg && cat /usr/bin/gpg
      '';
      branches = "master develop ETCM-165-publish";
    };

    install-base64 = commonAttrs // {
      label = "Install Base64";
      command = ''
        which base64; yum install -y cl-base64
      '';
      branches = "master develo ETCM-165-publishp";
    };

    publish = commonAttrs // {
      dependsOn = [ install-gpg install-base64 test-crypto test-rlp ];
      label = "Publishing libraries to Maven";
      command = ''
        nix-shell --run './publish.sh'
      '';
      branches = "master develop ETCM-165-publish";
      timeoutInMinutes = 60;
    };
  };
}
