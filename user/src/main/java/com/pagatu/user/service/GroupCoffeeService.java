package com.pagatu.user.service;

import com.pagatu.user.entity.GroupCoffee;
import com.pagatu.user.repository.GroupCofeeRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupCoffeeService {

    private GroupCofeeRepository groupCofeeRepository;

    public void setGroupCofeeRepository(GroupCofeeRepository groupCofeeRepository) {
        this.groupCofeeRepository = groupCofeeRepository;
    }

    public void save(GroupCoffee groupCoffee) {
        if (groupCofeeRepository.findByName(groupCoffee.getName()).isPresent()) {
            throw new RuntimeException("Group Coffee already exist: " + groupCoffee.getName());
        }
        groupCofeeRepository.save(groupCoffee);
    }

    public GroupCoffee findById(Long id) {
        return groupCofeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Group Coffee not found with id: " + id));
    }

    public GroupCoffee update(Long id, GroupCoffee updatedGroupCoffee) {
        GroupCoffee existing = findById(id);

        existing.setName(updatedGroupCoffee.getName());
        existing.setDescription(updatedGroupCoffee.getDescription());

        // set user many to many mapping
        existing.setUsers(updatedGroupCoffee.getUsers());

        return groupCofeeRepository.save(existing);
    }

    public void delete(Long id) {
        GroupCoffee existing = findById(id);
        groupCofeeRepository.delete(existing);
    }

}
