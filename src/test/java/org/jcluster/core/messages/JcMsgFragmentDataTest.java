/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package org.jcluster.core.messages;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author platar86
 */
public class JcMsgFragmentDataTest {

    JcMsgFragmentData instance;

    public JcMsgFragmentDataTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        instance = new JcMsgFragmentData("testFfrgId", 10, 20, new byte[55]);
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of getFrgId method, of class JcMsgFragmentData.
     */
    @Test
    public void testGetFrgId() {
        System.out.println("getFrgId");
        String expResult = "testFfrgId";
        String result = instance.getFrgId();
        assertEquals(expResult, result);

    }

    /**
     * Test of getSeq method, of class JcMsgFragmentData.
     */
    @Test
    public void testGetSeq() {
        System.out.println("getSeq");

        int expResult = 10;
        int result = instance.getSeq();
        assertEquals(expResult, result);

    }

    /**
     * Test of getData method, of class JcMsgFragmentData.
     */
    @Test
    public void testGetData() {
        System.out.println("getData");

        byte[] expResult = new byte[55];
        byte[] result = instance.getData();
        assertArrayEquals(expResult, result);

    }

    /**
     * Test of getTotalFragments method, of class JcMsgFragmentData.
     */
    @Test
    public void testGetTotalFragments() {
        System.out.println("getTotalFragments");

        int expResult = 20;
        int result = instance.getTotalFragments();
        assertEquals(expResult, result);

    }

}
