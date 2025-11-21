package com.hotelmanagement.quanlikhachsan.model.room;


import jakarta.persistence.*;
import lombok.*;

@Table(name = "room_types")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String name;

    private String description;

    private Double pricePerNight;

//    @OneToMany(mappedBy = "roomType")
//    @Builder.Default
//    private List<Room> rooms = new ArrayList<>();
}
