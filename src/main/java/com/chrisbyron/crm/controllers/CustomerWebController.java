package com.chrisbyron.crm.controllers;

import com.chrisbyron.crm.data.entities.Customer;
import com.chrisbyron.crm.data.repositories.CustomerRepository;
import com.chrisbyron.crm.services.CustomerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerWebController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @GetMapping
    public String index() {
        return "/customer/index.html";
    }

    @GetMapping(value = "/data_for_datatable", produces = "application/json")
    @ResponseBody
    public String getDataForDatatable(@RequestParam Map<String, Object> params) {
        final int draw = params.containsKey("draw") ? Integer.parseInt(params.get("draw").toString()) : 1;
        final int length = params.containsKey("length") ? Integer.parseInt(params.get("length").toString()) : 30;
        final int start = params.containsKey("start") ? Integer.parseInt(params.get("start").toString()) : 30;
        final int currentPage = start / length;

        String sortName = "id";
        final String dataTableOrderColumnIdx = params.get("order[0][column]").toString();
        final String dataTableOrderColumnName = "columns[" + dataTableOrderColumnIdx + "][data]";
        if (params.containsKey(dataTableOrderColumnName))
            sortName = params.get(dataTableOrderColumnName).toString();
        String sortDir = params.containsKey("order[0][dir]") ? params.get("order[0][dir]").toString() : "asc";

        final Sort.Order sortOrder = new Sort.Order((sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC), sortName);
        final Sort sort = Sort.by(sortOrder);

        final Pageable pageRequest = PageRequest.of(currentPage,
                length,
                sort);

        final String queryString = (String) (params.get("search[value]"));

        final Page<Customer> customers = customerService.getCustomersForDatatable(queryString, pageRequest);

        final long totalRecords = customers.getTotalElements();

        final List<Map<String, Object>> cells = new ArrayList<>();
        customers.forEach(customer -> {
            Map<String, Object> cellData = new HashMap<>();
            cellData.put("id", customer.getId());
            cellData.put("firstName", customer.getFirstName());
            cellData.put("lastName", customer.getLastName());
            cellData.put("emailAddress", customer.getEmailAddress());
            cellData.put("city", customer.getCity());
            cellData.put("country", customer.getCountry());
            cellData.put("phoneNumber", customer.getPhoneNumber());
            cells.add(cellData);
        });

        final Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("draw", draw);
        jsonMap.put("recordsTotal", totalRecords);
        jsonMap.put("recordsFiltered", totalRecords);
        jsonMap.put("data", cells);

        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }


    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model) {
        final Customer customerInstance = customerRepository.findById(Long.valueOf(id)).get();

        model.addAttribute("customerInstance", customerInstance);

        return "/customer/edit.html";
    }

    @PostMapping("/update")
    public String update(@Valid @ModelAttribute("customerInstance") Customer customerInstance,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes atts) {
        if (bindingResult.hasErrors()) {
            return "/customer/edit.html";
        } else {
            if (customerRepository.save(customerInstance) != null)
                atts.addFlashAttribute("message", "Customer updated successfully");
            else
                atts.addFlashAttribute("message", "Customer update failed.");

            return "redirect:/customer";
        }
    }

    @GetMapping("/create")
    public String create(Model model)
    {
        model.addAttribute("customerInstance", new Customer());
        return "/customer/create.html";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("customerInstance") Customer customerInstance,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes atts) {
        if (bindingResult.hasErrors()) {
            return "/customer/create.html";
        } else {
            if (customerRepository.save(customerInstance) != null)
                atts.addFlashAttribute("message", "Customer created successfully");
            else
                atts.addFlashAttribute("message", "Customer creation failed.");

            return "redirect:/customer";
        }
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes atts) {
        Customer customerInstance = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer Not Found:" + id));

        customerRepository.delete(customerInstance);

        atts.addFlashAttribute("message", "Customer deleted.");

        return "redirect:/customer";
    }
}
