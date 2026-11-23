package com.bookworm.web;

import com.bookworm.dto.Dto.AddressDto;
import com.bookworm.dto.Dto.AddressRequest;
import com.bookworm.service.AddressService;
import com.bookworm.service.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public List<AddressDto> list() {
        return addresses.list(CurrentUser.requireId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto create(@Valid @RequestBody AddressRequest req) {
        return addresses.create(CurrentUser.requireId(), req);
    }

    @GetMapping("/{addressId}")
    public AddressDto get(@PathVariable Long addressId) {
        return addresses.get(CurrentUser.requireId(), addressId);
    }

    @PutMapping("/{addressId}")
    public AddressDto update(@PathVariable Long addressId, @Valid @RequestBody AddressRequest req) {
        return addresses.update(CurrentUser.requireId(), addressId, req);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long addressId) {
        addresses.delete(CurrentUser.requireId(), addressId);
    }
}
