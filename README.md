# Qubership Testing Platform Configuration Dataset Excel Library

## Purpose

Configuration Dataset Excel Library is used currently in Qubership Testing Platform ITF-Executor service, to process excel dataset files.
These are files of special format where:
- Each Sheet is DataSet List,
- The 1st and the 2nd column of a Sheet contain group names and parameter names correspondingly,
- The 1st row of a Sheet contains DataSet names, starting from the 3rd column,
- All other rows contain parameters' values, starting from the 3rd column.

## How to add dependency into a service
```xml
    <!-- Change version number if necessary -->
    <dependency>
        <groupId>org.qubership.atp</groupId>
        <artifactId>configuration-dataset-excel</artifactId>
        <version>3.5.0-SNAPSHOT</version>
    </dependency>
```

## Local build

In IntelliJ IDEA, one can select 'github' Profile in Maven Settings menu on the right, then expand Lifecycle dropdown of qubership-atp-configuration-dataset-excel module, then select 'clean' and 'install' options and click 'Run Maven Build' green arrow button on the top.

Or, one can execute the command:
```bash
mvn -P github clean install
```

## Functionality

Configuration Dataset Excel Library can:
- read and manage Cells, DataSets, DatasetLists; 
- evaluate Formulas; 
- track changes (including reverting of them);
- process external links.

It uses Apache POI libraries:
```xml
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>4.1.2</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>4.1.2</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml-schemas</artifactId>
        <version>4.1.2</version>
    </dependency>
```

