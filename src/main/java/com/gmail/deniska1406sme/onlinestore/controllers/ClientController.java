package com.gmail.deniska1406sme.onlinestore.controllers;

import com.gmail.deniska1406sme.onlinestore.config.JwtTokenProvider;
import com.gmail.deniska1406sme.onlinestore.dto.*;
import com.gmail.deniska1406sme.onlinestore.model.OrderStatus;
import com.gmail.deniska1406sme.onlinestore.model.UserRole;
import com.gmail.deniska1406sme.onlinestore.services.ClientService;
import com.gmail.deniska1406sme.onlinestore.services.EmployeeService;
import com.gmail.deniska1406sme.onlinestore.services.OrderService;
import com.gmail.deniska1406sme.onlinestore.services.PasswordAuthenticationService;
import com.gmail.deniska1406sme.onlinestore.validation.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordAuthenticationService passwordAuthenticationService;

    @Autowired
    public ClientController(ClientService clientService, EmployeeService employeeService, OrderService orderService,
                            JwtTokenProvider jwtTokenProvider, PasswordAuthenticationService passwordAuthenticationService) {
        this.clientService = clientService;
        this.employeeService = employeeService;
        this.orderService = orderService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordAuthenticationService = passwordAuthenticationService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization") String token) {
        String email = jwtTokenProvider.getLogin(token);
        if (jwtTokenProvider.getRole(token) == UserRole.CLIENT) {
            ClientDTO clientDTO = clientService.getClientByEmail(email);
            return ResponseEntity.ok(clientDTO);
        } else if (jwtTokenProvider.getRole(token) == UserRole.EMPLOYEE) {
            EmployeeDTO employeeDTO = employeeService.getEmployeeByEmail(email);
            return ResponseEntity.ok(employeeDTO);
        }
        return ResponseEntity.badRequest().build();
    }

    @PatchMapping("/profile/change-password")
    public ResponseEntity<List<String>> changePassword(@RequestHeader(value = "Authorization") String token,
                                                       @RequestBody @Validated ChangePasswordRequest request,
                                                       BindingResult bindingResult) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String email = jwtTokenProvider.getLogin(token);
        passwordAuthenticationService.changePassword(email, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/profile/set-password")
    public ResponseEntity<Void> setClientPassword(@RequestHeader(value = "Authorization") String token,
                                                  @RequestBody @NotBlank String rawPassword) {
        String email = jwtTokenProvider.getLogin(token);
        passwordAuthenticationService.savePassword(email, rawPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile/has-password")
    public ResponseEntity<Boolean> hasPassword(@RequestHeader(value = "Authorization") String token) {
        String email = jwtTokenProvider.getLogin(token);
        return ResponseEntity.ok(passwordAuthenticationService.hasPassword(email));
    }

    @CacheEvict(value = "employees", allEntries = true)
    @PatchMapping("/profile/update")
    public ResponseEntity<List<String>> updateClient(@RequestHeader(value = "Authorization") String token,
                                                     @RequestBody @Validated(OnUpdate.class) ClientDTO clientDTO,
                                                     BindingResult bindingResult) {
        String email = jwtTokenProvider.getLogin(token);
        if (jwtTokenProvider.getRole(token) == UserRole.CLIENT) {
            Long id = clientService.getClientByEmail(email).getId();
            clientService.updateClient(clientDTO, id);
        } else if (jwtTokenProvider.getRole(token) == UserRole.EMPLOYEE) {
            EmployeeDTO employeeDTO = new EmployeeDTO(clientDTO.getFirstName(), clientDTO.getLastName(), clientDTO.getPhone());
            Long id = employeeService.getEmployeeByEmail(email).getId();
            employeeService.updateEmployee(employeeDTO, id);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderDTO>> getOrders(@RequestHeader(value = "Authorization") String token,
                                                    Pageable pageable) {
        String email = jwtTokenProvider.getLogin(token);
        Long id = clientService.getClientByEmail(email).getId();
        Page<OrderDTO> orders = orderService.getOrdersByClient(id, pageable);
        for (OrderDTO order : orders) {
            order.setClientFirstName(clientService.getClientByEmail(email).getFirstName());
            order.setClientLastName(clientService.getClientByEmail(email).getLastName());
        }
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/order/cancel-order/{id}")
    public ResponseEntity<Void> cancelOrder(@RequestHeader(value = "Authorization") String token,
                                            @PathVariable Long id) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(id);
        orderDTO.setOrderStatus(OrderStatus.CANCELED);
        orderService.updateOrder(orderDTO, id);
        return ResponseEntity.ok().build();
    }
}
