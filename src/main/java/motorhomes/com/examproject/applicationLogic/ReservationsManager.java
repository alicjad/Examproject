package motorhomes.com.examproject.applicationLogic;


import motorhomes.com.examproject.model.*;
import motorhomes.com.examproject.repositories.*;

import java.net.Inet4Address;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ Pawel Pohl
 */
public class ReservationsManager {

    private CustomersDbRepository customersRepository;
    private ReservationsRepository reservationsRepository;
    private MotorhomeDbRepository motorhomesRepository;
    private PickupDbRepository pickupDbRepository;
    private DropOffDbRepository dropOffDbRepository;
    private AccessoriesDbRepository accessoriesDbRepository;
    private ReservationsAccessoriesRepository reservationsAccessoriesRepository;

    public ReservationsManager(CustomersDbRepository customersRepository, ReservationsRepository reservationsRepository,
                               MotorhomeDbRepository motorhomesRepository, PickupDbRepository pickupDbRepository,
                               DropOffDbRepository dropOffDbRepository, AccessoriesDbRepository accessoriesDbRepository,
                               ReservationsAccessoriesRepository reservationsAccessoriesRepository) {
        this.customersRepository = customersRepository;
        this.reservationsRepository = reservationsRepository;
        this.motorhomesRepository = motorhomesRepository;
        this.pickupDbRepository = pickupDbRepository;
        this.dropOffDbRepository = dropOffDbRepository;
        this.accessoriesDbRepository = accessoriesDbRepository;
        this.reservationsAccessoriesRepository = reservationsAccessoriesRepository;
    }

    public List<Reservation> getReservations() {
        try {
            List<Reservation> reservations = reservationsRepository.readAll();
            return reservations;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    public Reservation startReservation(LocalDate startDate, LocalDate endDate) {
        Reservation reservation = new Reservation();
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setStatus("Not Payed");
        return reservation;
    }

    public List<Motorhome> getAvailableMotorhomes(Reservation reservation) {
        try {
            List<Integer> availableMotorhomesIds = motorhomesRepository.readAllIDs();
            reservationsRepository.getAvailableMotorhomesIds(reservation.getStartDate(), reservation.getEndDate(), availableMotorhomesIds);
            return motorhomesRepository.readAll(availableMotorhomesIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveMotorhome(Reservation reservation, int motorhomesId) {
        reservation.setMotorhomeId(motorhomesId);
        return true;
    }

    public List<Accessory> getAccessories() {
        try {
            return accessoriesDbRepository.readAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveCustomer(Reservation reservation, Customer customer) {
        try {
            Customer savedCustomer = customersRepository.read(customer.getDrivingLicenseNr());
            if (savedCustomer == null) {
                customersRepository.create(customer);
                savedCustomer = customersRepository.read(customer.getDrivingLicenseNr());
            }
            reservation.setCustomerId(savedCustomer.getCustomerId());
            int reservationId = reservationsRepository.create(reservation);
            reservation.setReservationId(reservationId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean saveTransport(Reservation reservation, int dropDistance, String dropLocation, int pickDistance, String pickLocation) {
        try {
            if (dropDistance > 0) {
                DropOff dropOff = new DropOff();
                reservation.setHasDropOff(true);
                dropOff.setDropOffDistance(dropDistance);
                dropOff.setDropOffLocation(dropLocation.trim());
                dropOffDbRepository.create(dropOff, reservation.getReservationId());
            }
                if (pickDistance > 0) {
                PickUp pickUp = new PickUp();
                reservation.setHasPickUp(true);
                pickUp.setPickUpDistance(pickDistance);
                pickUp.setPickUpLocation(pickLocation.trim());
                pickupDbRepository.create(pickUp, reservation.getReservationId());
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveAccessories(Reservation reservation, int[] quantities, List<Accessory> accessories) {
        Map<Integer, Integer> accessoryIdToQuantity = new HashMap<>();
        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i] > 0) {
                accessoryIdToQuantity.put(accessories.get(i).getId(), quantities[i]);
                try {
                    reservationsAccessoriesRepository.create(reservation.getReservationId(), accessories.get(i).getId(), quantities[i]);
                } catch (SQLException e) {
                    return false;
                }
                if (!reservation.isHasAccessories()) {
                    reservation.setHasAccessories(true);
                }
            }

        }
        reservation.setAccessories(accessoryIdToQuantity);
        return true;
    }

    public boolean updateReservation(Reservation reservation) {
        try {
            reservationsRepository.update(reservation);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Reservation getReservation(int reservationId) {
        try {
            Reservation reservation;
            reservation = reservationsRepository.read(reservationId);
            reservation.setAccessories(reservationsAccessoriesRepository.readAll(reservation.getReservationId()));
            return reservation;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public DropOff getDropOff (int reservationId) {
        try {
            return dropOffDbRepository.read(reservationId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PickUp getPickUp (int reservationId) {
        try {
            return pickupDbRepository.read(reservationId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Customer getCustomer (int customerId) {
        try {
            return customersRepository.read(customerId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Motorhome getMotorhome (int motorhomeId) {
        try {
            return motorhomesRepository.read(motorhomeId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Accessory getAccessory(int accessoryId) {
        try {
            return accessoriesDbRepository.read(accessoryId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateReservation(Reservation reservation, String newStatus, int newPrice, PickUp oldPickUp,
                                     PickUp newPickUp, DropOff oldDropOff, DropOff newDropOff, int[] newQuantities, Map<Accessory, Integer> accessoryQuantityMap) {

        boolean shouldChangePrice = true;
        if (!reservation.getStatus().equals(newStatus)) {
            reservation.setStatus(newStatus);
        }
        try {

            if (newPickUp.getPickUpDistance() > 0) {
                if (oldPickUp.getPickUpDistance() > 0) {
                    if (oldPickUp.getPickUpDistance() != newPickUp.getPickUpDistance() || !oldPickUp.getPickUpLocation().equals(newPickUp.getPickUpLocation())) {
                        newPickUp.setPickUpId(oldPickUp.getPickUpId());
                        pickupDbRepository.update(newPickUp);
                        shouldChangePrice = false;
                    }
                } else {
                    pickupDbRepository.create(newPickUp, reservation.getReservationId());
                    reservation.setHasPickUp(true);
                    shouldChangePrice = false;
                }
            } else {
                if (oldPickUp.getPickUpDistance() > 0) {
                    pickupDbRepository.delete(oldPickUp.getPickUpId());
                    reservation.setHasPickUp(false);
                    shouldChangePrice = false;
                }
            }

            if (newDropOff.getDropOffDistance() > 0) {
                if (oldDropOff.getDropOffDistance() > 0) {
                    if (oldDropOff.getDropOffDistance() != newDropOff.getDropOffDistance() || !oldDropOff.getDropOffLocation().equals(newDropOff.getDropOffLocation())) {
                        newDropOff.setDropOffId(oldDropOff.getDropOffId());
                        dropOffDbRepository.update(newDropOff);
                        shouldChangePrice = false;
                    }
                } else {
                    reservation.setHasDropOff(true);
                    dropOffDbRepository.create(newDropOff, reservation.getReservationId());
                    shouldChangePrice = false;
                }
            } else {
                if (oldDropOff.getDropOffDistance() > 0) {
                    reservation.setHasDropOff(false);
                    dropOffDbRepository.delete(oldDropOff.getDropOffId());
                    shouldChangePrice = false;
                }
            }

            int i = 0;
            boolean hasAccessories = false;
            for (Map.Entry<Accessory, Integer> entry : accessoryQuantityMap.entrySet()) {
                if (entry.getValue() == 0) {
                    if (newQuantities[i] > 0) {
                        reservationsAccessoriesRepository.create(reservation.getReservationId(), entry.getKey().getId(), newQuantities[i]);
                    }
                } else {
                    if (newQuantities[i] == 0) {
                        reservationsAccessoriesRepository.delete(reservation.getReservationId(), entry.getKey().getId());
                    } else {
                        reservationsAccessoriesRepository.update(reservation.getReservationId(), entry.getKey().getId(), newQuantities[i]);
                    }
                }
                if (newQuantities[i] > 0) {
                    hasAccessories = true;
                }
                i++;
            }
            reservation.setHasAccessories(hasAccessories);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        //todo update price

        if (shouldChangePrice) {
            reservation.setPrice(newPrice);
        }
        return this.updateReservation(reservation);
    }


    private void calculatePrice() {

    }
}
