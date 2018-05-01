package hu.nemi.store

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

/**
 * For compatibility and correctness uses the same test set as Arrow Lens documentation
 */
class LensTest {
    @Test
    fun `can access nested properties`() {
        val employeeStreetName: Lens<Employee, String> = employeeCompany + companyAddress + addressStreet + streetName
        val employee = Employee("John Doe", Company("Arrow", Address("Functional city", Street(23, "lambda street"))))
        val expectedEmployee = Employee("John Doe", Company("Arrow", Address("Functional city", Street(23, "Lambda street"))))

        assertThat(employeeStreetName(employee, String::capitalize))
                .isEqualTo(expectedEmployee)
    }
}

private data class Street(val number: Int, val name: String)
private data class Address(val city: String, val street: Street)
private data class Company(val name: String, val address: Address)
private data class Employee(val name: String, val company: Company)

private val employeeCompany: Lens<Employee, Company> = Lens(
        get = { it.company },
        set = { company -> { employee -> employee.copy(company = company) } }
)

private val companyAddress: Lens<Company, Address> = Lens(
        get = { it.address },
        set = { address -> { company -> company.copy(address = address) } }
)

private val addressStreet: Lens<Address, Street> = Lens(
        get = { it.street },
        set = { street -> { address -> address.copy(street = street) } }
)

private val streetName: Lens<Street, String> = Lens(
        get = { it.name },
        set = { name -> { street -> street.copy(name = name) } }
)