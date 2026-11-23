package com.bookworm.service;

import com.bookworm.domain.Address;
import com.bookworm.dto.Dto.AddressDto;
import com.bookworm.dto.Dto.AddressRequest;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.AddressRepository;
import com.bookworm.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addresses;
    private final UserRepository users;

    public AddressService(AddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AddressDto> list(Long userId) {
        return addresses.findByUserIdOrderByIdAsc(userId).stream().map(AddressService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AddressDto get(Long userId, Long id) {
        return toDto(find(userId, id));
    }

    public AddressDto create(Long userId, AddressRequest req) {
        Address a = new Address();
        a.setUser(users.getReferenceById(userId));
        apply(a, req);
        // The first address a user saves becomes the default.
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault()) || addresses.findByUserIdOrderByIdAsc(userId).isEmpty();
        if (makeDefault) {
            clearDefaults(userId);
        }
        a.setDefaultAddress(makeDefault);
        return toDto(addresses.save(a));
    }

    public AddressDto update(Long userId, Long id, AddressRequest req) {
        Address a = find(userId, id);
        apply(a, req);
        if (Boolean.TRUE.equals(req.isDefault())) {
            clearDefaults(userId);
            a.setDefaultAddress(true);
        } else if (req.isDefault() != null) {
            a.setDefaultAddress(false);
        }
        return toDto(addresses.save(a));
    }

    public void delete(Long userId, Long id) {
        addresses.delete(find(userId, id));
    }

    /** Resolves a saved address that belongs to the user. */
    @Transactional(readOnly = true)
    public Address require(Long userId, Long id) {
        return find(userId, id);
    }

    private Address find(Long userId, Long id) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Address not found"));
    }

    private void clearDefaults(Long userId) {
        addresses.findByUserIdOrderByIdAsc(userId).stream().filter(Address::isDefaultAddress).forEach(x -> {
            x.setDefaultAddress(false);
            addresses.save(x);
        });
    }

    private static void apply(Address a, AddressRequest r) {
        a.setFirstName(r.firstName().trim());
        a.setLastName(r.lastName().trim());
        a.setEmail(r.email().trim());
        a.setPhoneCountryCode(r.phoneCountryCode());
        a.setPhone(r.phone());
        a.setAddressLine(r.addressLine().trim());
        a.setCity(r.city().trim());
        a.setState(r.state().trim());
        a.setPinCode(r.pinCode().trim());
        a.setCountry(r.country().trim());
    }

    public static AddressDto toDto(Address a) {
        return new AddressDto(a.getId(), a.getFirstName(), a.getLastName(), a.getEmail(), a.getPhoneCountryCode(),
                a.getPhone(), a.getAddressLine(), a.getCity(), a.getState(), a.getPinCode(), a.getCountry(),
                a.isDefaultAddress());
    }
}
