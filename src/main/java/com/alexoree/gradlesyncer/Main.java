/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alexoree.gradlesyncer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author alex
 */
public class Main {

     static Properties props = new Properties();

     public static void main(String[] args) throws Exception {

          String rootpath = args[0];
             
          
          //recursive search for *.gradle files that are NOT in a build file.
          List<File> files = scan(rootpath); //${project.property('

          File gradle = new File(rootpath + "/gradle.properties");
          if (gradle.exists()) {    //god i hope not
               props.load(new FileInputStream(gradle));
          }
          gradle = new File(rootpath + "/local.properties");
          if (gradle.exists()) {
               props.load(new FileInputStream(gradle));
          }
          //search for anything along the lines of
          //compile
          //provided
          //testCompile etc
          //androidTestCompile
          //android variants
          //all of of the different syntaxes used for this
          //ignore project dependencies
          //ignore exclusions?
          
          //TODO look for repositories!!
          Set<Dependency> deps = new HashSet<>();
          for (int i = 0; i < files.size(); i++) {
               File get = files.get(i);
               deps.addAll(parseGradleFile(get));
          }

          Iterator<Dependency> iterator = deps.iterator();
          while (iterator.hasNext()) {
               Dependency next = iterator.next();
               System.out.println(next.toString());
          }

          generatePom(deps);
          //then generate a bonus pom

     }
     
     
     //recursive search for *.gradle files that are NOT in a build dir.
     private static List<File> scan(String arg) {
          List<File> ret = new ArrayList<>();
          File root = new File(arg);
          if (!root.exists()) {
               return ret;
          }
          if (!root.isDirectory()) {
               return ret;
          }
          File[] listFiles = root.listFiles(new FileFilter() {
               @Override
               public boolean accept(File pathname) {
                    if (pathname.isHidden()) {
                         return false;
                    }
                    if (pathname.getName().toLowerCase().endsWith(".gradle")) {
                         return true;
                    }
                    if (pathname.isDirectory()) {
                         return true;
                    }
                    return false;
               }
          });
          if (listFiles == null) {
               return ret;
          }
          if (listFiles.length == 0) {
               return ret;
          }
          for (int i = 0; i < listFiles.length; i++) {
               File f = listFiles[i];
               if (f.isHidden()) {
                    continue;
               }
               if (f.getName().startsWith(".")) {
                    continue;
               }
               if (f.getName().equalsIgnoreCase("build")) {
                    continue;
               }
               if (f.isDirectory()) {
                    ret.addAll(scan(f.getAbsolutePath()));
               }
               if (f.getName().endsWith(".gradle")) {
                    ret.add(f);
               }
          }

          return ret;
     }

     private static Set<Dependency> parseGradleFile(File file) {

          Set<Dependency> ret = new HashSet<>();
          //boolean deps = false;
          int braceCount = 0;
          try (BufferedReader br = new BufferedReader(new FileReader(file))) {
               String line;
               while ((line = br.readLine()) != null) {
                    // process the line.

                    if (true) {
                         //
                         //if (line.toLowerCase().contains("compile"))
                         {
                              //typically, dependencies are 1 liners, except if there is a dependency
//testCompile "junit:junit:${project.property('junit.version')}"
//testCompile group: 'junit', name: 'junit', version: '4.8.2'
                              if (line.trim().startsWith("//")) {

                              } else if (line.trim().startsWith("apply from") && line.toLowerCase().contains("http")) {
                                   //TODO

                              } else if (line.toLowerCase().contains("compile")
                                   || line.toLowerCase().contains("runtime")
                                   || line.toLowerCase().contains("androidTestCompile")
                                   || line.toLowerCase().contains("provided")
                                   || line.toLowerCase().contains("classpath")) {
                                   //System.out.println(line);
                                   Dependency d = new Dependency();
                                   if (line.trim().startsWith("testCompile")) {
                                        d.classifier = "test";
                                   } else if (line.trim().startsWith("compileOnly")) {
                                        d.classifier = "runtime";
                                   }
                                   if (line.contains("compileOnly")) {
                                        System.out.println();
                                   }
                                   line = line.trim();
                                   line = line.replace("\"", "");
                                   line = line.replace("'", "");
                                   line = line.replace("(", "");
                                   line = line.replace(")", "");
                                   line = line.replace("{", "");
                                   line = line.replace("}", "");
                                   String[] s = line.split(" ");
                                   //s[0] = test/Compile, etc
                                   //s[1] = the dep def
                                   //commons-io:commons-io:${project.property('commons-io.version')}
                                   //org.osmdroid:osmdroid-android:5.2@aar
                                   if (s.length >= 2) {
                                        String[] bits = s[1].split(":");
                                        if (bits.length == 3) {
                                             //simple case
                                             d.groupId = bits[0];
                                             d.artifactId = bits[1];
                                             d.version = bits[2];
                                             if (d.version.contains("$")) {
                                                  //yay from gradle magic
                                                  d.version = d.version.replace("$project.property", "");
                                                  d.version = d.version.replace("\"", "");
                                                  d.version = d.version.replace("'", "");
                                                  d.version = props.getProperty(d.version);

                                             }
                                             if (d.version == null) {
                                                  d.version = "+";
                                                  System.out.println();
                                             }
                                             if (d.version.contains("+")) {
                                                  //a gradle wildcard
                                                  if ("+".equals(d.version)) {
                                                       d.version = "LATEST";
                                                  } else if (d.version.endsWith(".+")) {
                                                       //the version is something.+
                                                       d.version = "[" + d.version.replace(".+", ",)");
                                                  } else if (d.version.endsWith("+")) {
                                                       d.version = "[" + d.version.replace("+", ",)");
                                                  } else {

                                                  }
                                             }
                                             if (d.version.contains("@")) {
                                                  String[] x = d.version.split("@");
                                                  d.version = x[0];
                                                  d.type = x[1];
                                             }
                                        } else if (bits.length == 4) {
                                             //with classifier
                                             //simple case
                                             d.groupId = bits[0];
                                             d.artifactId = bits[1];
                                             d.version = bits[2];
                                             if (d.version.contains("$")) {
                                                  //yay from gradle magic
                                                  d.version = d.version.replace("$project.property", "");
                                                  //d.version = d.version.replace(")}", "");
                                                  d.version = d.version.replace("\"", "");
                                                  d.version = d.version.replace("'", "");
                                                  d.version = props.getProperty(d.version);

                                             }
                                             if (d.version == null) {
                                                  d.version = "+";
                                                  System.out.println();
                                             }
                                             if (d.version.contains("+")) {
                                                  //a gradle wildcard
                                                  if ("+".equals(d.version)) {
                                                       d.version = "LATEST";
                                                  } else if (d.version.endsWith(".+")) {
                                                       //the version is something.+
                                                       d.version = "[" + d.version.replace(".+", ",)");
                                                  } else if (d.version.endsWith("+")) {
                                                       d.version = "[" + d.version.replace("+", ",)");
                                                  } else {

                                                  }
                                             }
                                             d.classifier = bits[3];
                                             if (d.classifier.contains("@")) {
                                                  String[] x = d.classifier.split("@");
                                                  d.classifier = x[0];
                                                  d.type = x[1];
                                             }
                                        } else if (bits.length == 7) {
                                             ////testCompile group: 'junit', name: 'junit', version: '4.8.2'
                                             //with classifier
                                             //simple case
                                             if (bits[1].equalsIgnoreCase("group:")) {
                                                  d.groupId = bits[2];
                                             } else if (bits[1].equalsIgnoreCase("name:")) {
                                                  d.artifactId = bits[2];
                                             } else if (bits[1].equalsIgnoreCase("version:")) {
                                                  d.version = bits[2];
                                             }

                                             if (bits[3].equalsIgnoreCase("group:")) {
                                                  d.groupId = bits[4];
                                             } else if (bits[3].equalsIgnoreCase("name:")) {
                                                  d.artifactId = bits[4];
                                             } else if (bits[3].equalsIgnoreCase("version:")) {
                                                  d.version = bits[4];
                                             }

                                             if (bits[5].equalsIgnoreCase("group:")) {
                                                  d.groupId = bits[6];
                                             } else if (bits[5].equalsIgnoreCase("name:")) {
                                                  d.artifactId = bits[6];
                                             } else if (bits[5].equalsIgnoreCase("version:")) {
                                                  d.version = bits[6];
                                             }

                                             d.artifactId = d.artifactId.replace("'", "");
                                             d.artifactId = d.artifactId.replace("\"", "");
                                             d.groupId = d.groupId.replace("\"", "");
                                             d.groupId = d.groupId.replace("'", "");
                                             d.version = d.version.replace("\"", "");
                                             d.version = d.version.replace("'", "");
                                             if (d.version.contains("$")) {
                                                  //yay from gradle magic
                                                  d.version = d.version.replace("$project.property", "");
                                                  //d.version = d.version.replace(")}", "");
                                                  d.version = d.version.replace("\"", "");
                                                  d.version = d.version.replace("'", "");
                                                  d.version = props.getProperty(d.version);

                                             }
                                             if (d.version == null) {
                                                  d.version = "+";
                                                  System.out.println();
                                             }
                                             if (d.version.contains("+")) {
                                                  //a gradle wildcard
                                                  if ("+".equals(d.version)) {
                                                       d.version = "LATEST";
                                                  } else if (d.version.endsWith(".+")) {
                                                       //the version is something.+
                                                       d.version = "[" + d.version.replace(".+", ",)");
                                                  } else if (d.version.endsWith("+")) {
                                                       d.version = "[" + d.version.replace("+", ",)");
                                                  } else {

                                                  }
                                             }
                                             d.classifier = bits[3];
                                             if (d.classifier.contains("@")) {
                                                  String[] x = d.classifier.split("@");
                                                  d.classifier = x[0];
                                                  d.type = x[1];
                                             }

                                        }    //probably a case for 9, with type?     
                                        if (d.version == null) {
                                             System.out.print("");
                                        } else {

                                             ret.add(d);
                                        }
                                   }
                              }

                         }
                    }
               }
          } catch (Exception ex) {
               ex.printStackTrace();
          }
          //search for anything along the lines of
          //compile
          //provided
          //testCompile etc
          //androidTestCompile
          //android variants
          //all of of the different syntaxes used for this
          //ignore project dependencies
          //ignore exclusions?
          //also classpath entries
          //also gradle plugin declarations 

          return ret;
     }

     private static void generatePom(Set<Dependency> deps) throws Exception{
          StringBuilder sb = new StringBuilder();
          sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
               + "    <modelVersion>4.0.0</modelVersion>\n"
               + "    <groupId>tempuri</groupId>\n"
               + "    <artifactId>tempuri</artifactId>\n"
               + "    <version>1.0.0-SNAPSHOT</version>\n"
               + "    <packaging>pom</packaging>\n"
               //TODO inject repositories that are common these days? 
               //or parse from the gradle files. ugh...
               + "\t<dependencies>\n");
          Iterator<Dependency> iterator = deps.iterator();
          while (iterator.hasNext()) {
               Dependency next = iterator.next();
               sb.append("\t\t<dependency>\n");
               sb.append("\t\t\t<groupId>").append(next.groupId).append("</groupId>\n");
               sb.append("\t\t\t<artifactId>").append(next.artifactId).append("</artifactId>\n");
               sb.append("\t\t\t<version>").append(next.version).append("</version>\n");
               if (next.classifier != null) {
                    sb.append("\t\t\t<classifier>").append(next.classifier).append("</classifier>\n");
               }
               if (next.type != null) {
                    sb.append("\t\t\t<type>").append(next.type).append("</type>\n");
               }
               sb.append("\t\t</dependency>\n");
          }

          sb.append("\t</dependencies>\n" + "</project>");
          
          File pom=new File("pom.xml");
          int x=1;
          while (pom.exists()){
               pom = new File ("pom" + x + ".xml");
               x++;
          }
               
          FileOutputStream fos = new FileOutputStream(pom);
          fos.write(sb.toString().getBytes());
          fos.close();
          System.out.println("Pom written to " + pom.getAbsolutePath());
          
     }

     static class Dependency {

          @Override
          public int hashCode() {
               int hash = 7;
               return hash;
          }

          @Override
          public boolean equals(Object obj) {
               if (this == obj) {
                    return true;
               }
               if (obj == null) {
                    return false;
               }
               if (getClass() != obj.getClass()) {
                    return false;
               }
               final Dependency other = (Dependency) obj;
               if (!Objects.equals(this.version, other.version)) {
                    return false;
               }
               if (!Objects.equals(this.groupId, other.groupId)) {
                    return false;
               }
               if (!Objects.equals(this.artifactId, other.artifactId)) {
                    return false;
               }
               if (!Objects.equals(this.classifier, other.classifier)) {
                    return false;
               }
               if (!Objects.equals(this.type, other.type)) {
                    return false;
               }
               return true;
          }

          String version;
          String groupId;
          String artifactId;
          String classifier;
          String type;

          public String toString() {
               return groupId + " " + artifactId + " " + version + " " + classifier + " " + type;
          }

     }

     
}
