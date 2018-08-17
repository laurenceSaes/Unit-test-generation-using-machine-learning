-- MySQL dump 10.13  Distrib 5.7.22, for Linux (x86_64)
--
-- Host: localhost    Database: unittests
-- ------------------------------------------------------
-- Server version	5.7.22-0ubuntu0.17.10.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `onePass`
--

DROP TABLE IF EXISTS `onePass`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `onePass` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_name` varchar(256) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_name` (`project_name`)
) ENGINE=InnoDB AUTO_INCREMENT=679 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queue`
--

DROP TABLE IF EXISTS `queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `queue` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_name` varchar(256) NOT NULL,
  `test_class` varchar(512) NOT NULL,
  `source_path` text NOT NULL,
  `class_path` longtext,
  `complete` tinyint(4) NOT NULL,
  `bussy` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `test_class` (`test_class`),
  KEY `complete` (`complete`,`bussy`)
) ENGINE=InnoDB AUTO_INCREMENT=337005 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `registration`
--

DROP TABLE IF EXISTS `registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `registration` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `test_method_id` int(11) DEFAULT NULL,
  `method_class` text,
  `method_name` text,
  `unit_test_method_class` text,
  `unit_test_method_name` text,
  `unit_test_code` text,
  `unit_test_code_sbt` text,
  `method_code` text,
  `method_code_sbt` text,
  PRIMARY KEY (`id`),
  KEY `registration_test_method_id_fk` (`test_method_id`),
  CONSTRAINT `registration_test_method_id_fk` FOREIGN KEY (`test_method_id`) REFERENCES `test_method` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=141009 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_method`
--

DROP TABLE IF EXISTS `test_method`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `test_method` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project` varchar(512) DEFAULT NULL,
  `class` varchar(512) DEFAULT NULL,
  `complete` tinyint(1) DEFAULT NULL,
  `error` tinyint(1) NOT NULL DEFAULT '0',
  `log` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `test_method_project_id_class_uindex` (`project`,`id`,`class`),
  UNIQUE KEY `test_method_class_project_uindex` (`class`,`project`)
) ENGINE=InnoDB AUTO_INCREMENT=66346 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-05-16  7:23:32
