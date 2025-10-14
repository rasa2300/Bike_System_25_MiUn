package com.bikeshare.lab4;

import com.bikeshare.service.validation.AgeValidator;
import com.bikeshare.service.validation.IDNumberValidator;
import com.bikeshare.service.auth.BankIDService;
import com.bikeshare.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

/**
 * Lab 4: Mutation Testing Improvement Template
 * 
 * GOAL: Increase mutation score by writing focused tests using mocks
 * 
 * STRATEGY:
 * 1. Run mutation testing: mvn clean test org.pitest:pitest-maven:mutationCoverage -Pcoverage-comparison
 * 2. Open target/pit-reports/index.html and analyze survived mutations (red lines)
 * 3. Write targeted tests using mocks to kill specific mutations
 * 4. Focus on boundary conditions, error scenarios, and logic verification
 * 5. Re-run mutation testing to measure improvement
 * 
 * HINTS:
 * - Look for mutations in AgeValidator.java (lines 43-44 are known survivors)
 * - Use mocks to create precise test scenarios
 * - Test both positive and negative cases
 * - Verify that tests fail when mutations are present
 * 
 * CURRENT BASELINE (from CoverageEx.java):
 * - AgeValidator: 94% line coverage, 75% mutation coverage
 * - 2 mutations survived in birthday logic
 * - Your job: Kill those mutations!
 * - Or Kill other mutations you find interesting
 * 
 * IF NOT POSSIBLE TO KILL THE MUTANTS? DEAD CODE? WHAT NOW?
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lab 4: Mutation Testing Improvement")
public class MutationImprovementTest {

    @Mock
    private IDNumberValidator mockIdValidator;
    
    @Mock
    private BankIDService mockBankIdService;
    
    @InjectMocks
    private AgeValidator ageValidator;
    
    @BeforeEach
    void setUp() {
        // MockitoExtensions handles mock initialization
        // Add any common setup here if needed
    }
    
    private static User newUser() {
        return new User("010101-1237", "user@example.com", "Bo", "Ek");
    }
    
    @Test
    @DisplayName("Should kill boundary mutation: exactly 18 years old")
    void shouldKillBoundaryMutationExactly18() {
        LocalDate today = LocalDate.now();
        String year = String.format("%04d", today.getYear() - 18);   // Get birth year (18 years ago)
        String month = String.format("%02d", today.getMonthValue()); // Current month
        String day = String.format("%02d", today.getDayOfMonth());   // Current day
        
        String pnr = year + month + day + "1234";
        
        when(mockIdValidator.isValidIDNumber(pnr)).thenReturn(true);
        when(mockBankIdService.authenticate(pnr)).thenReturn(true);

        boolean result = ageValidator.isAdult(pnr);

        assertTrue(result, "Person exactly 18 should be adult");
        verify(mockIdValidator).isValidIDNumber(pnr);
        verify(mockBankIdService).authenticate(pnr);
    }
    
    @Test
    @DisplayName("Should kill conditional mutation: invalid ID handling")
    void shouldKillConditionalMutation_InvalidId() {
        String invalidId = "invalid123";
        when(mockIdValidator.isValidIDNumber(invalidId)).thenReturn(false);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ageValidator.isAdult(invalidId)
        );
        
        assertEquals("Invalid ID number", exception.getMessage());
        verify(mockIdValidator).isValidIDNumber(invalidId);
        verifyNoInteractions(mockBankIdService); // Important: BankID not called
    }
    
    @Test
    @DisplayName("Should kill authentication mutation: auth failure handling")
    void shouldKillAuthenticationMutation_AuthFailure() {
        String validId = "200101010000";
        when(mockIdValidator.isValidIDNumber(validId)).thenReturn(true);
        when(mockBankIdService.authenticate(validId)).thenReturn(false);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ageValidator.isAdult(validId)
        );
        
        assertEquals("Authentication failed", exception.getMessage());
        verify(mockIdValidator).isValidIDNumber(validId);
        verify(mockBankIdService).authenticate(validId);
    }
    
    @Test
    @DisplayName("Should kill birthday logic mutation: person before birthday")
    void shouldKillBirthdayMutation_PersonBeforeBirthday() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        int birthYear = tomorrow.getYear() - 18; // 18 years ago
        int birthMonth = tomorrow.getMonthValue();
        int birthDay = tomorrow.getDayOfMonth();
        
        // ...what if we run this test on the 31st of december?
        // spooky non-deterministic tests!

        String year = String.format("%04d", birthYear);
        String month = String.format("%02d", birthMonth);
        String day = String.format("%02d", birthDay);
        
        String preBirthdayId = year + month + day + "1234";
        
        when(mockIdValidator.isValidIDNumber(preBirthdayId)).thenReturn(true);
        when(mockBankIdService.authenticate(preBirthdayId)).thenReturn(true);
        
        boolean result = ageValidator.isAdult(preBirthdayId);
        
        assertFalse(result, "Person who hasn't had their 18th birthday yet should not be adult");
        verify(mockIdValidator).isValidIDNumber(preBirthdayId);
        verify(mockBankIdService).authenticate(preBirthdayId);
    }
    
    // TODO: Write more tests for other mutation types
    // Hint: Look at the mutation report to see what other mutations exist
    // Examples: return value mutations, math operator mutations, etc.

    @Test
    @DisplayName("accepts minimum 0.01")
    void minimumAccepted() {
        var u = newUser();
        u.addFunds(0.01);
        assertEquals(0.01, u.getAccountBalance(), 1e-9);
    }

    @Test
    @DisplayName("rejects > 1000.00")
    void overMaxRejected() {
        var u = newUser();
        double before = u.getAccountBalance();
        assertThrows(IllegalArgumentException.class, () -> u.addFunds(1000.01));
        assertEquals(before, u.getAccountBalance(), 1e-9);
    }

    @Test
    @DisplayName("throws on insufficient balance")
    void insufficient() {
        var u = newUser();
        assertThrows(IllegalStateException.class, () -> u.deductFunds(1.0));
        assertEquals(0.0, u.getAccountBalance(), 1e-9);
    }
    
    // MEASUREMENT: After implementing your tests, run mutation testing again:
    // mvn clean test org.pitest:pitest-maven:mutationCoverage -Pmutation-demo
    // 
    // Compare your results:
    // - How many additional mutations did you kill?
    // - What's your new mutation coverage percentage?
    // - Which specific mutations are you most proud of killing?
}
