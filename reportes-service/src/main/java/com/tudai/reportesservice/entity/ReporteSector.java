package com.tudai.reportesservice.entity;
import jakarta.persistence.*;
@Entity
@Table(name="reportes_sector",uniqueConstraints=@UniqueConstraint(columnNames={
    "evento_id","sectorId"}
    ))
    public class ReporteSector {
        @Id
        @GeneratedValue(strategy=GenerationType.IDENTITY)
        private Long id;
        @ManyToOne(optional=false)
        @JoinColumn(name="evento_id")
        private ReporteEvento evento;
        private Long sectorId;
        private String nombre;
        private int capacidad;
        private long vendidas;
        private long devueltas;
        private long importeBrutoCentavos;
        private long importeReintegradoCentavos;
        private long importePendienteCentavos;
        public Long getSectorId(){
            return sectorId;
        }
        public void setSectorId(Long sectorId){
            this.sectorId=sectorId;
        }
        public void setEvento(ReporteEvento reporte){
            evento=reporte;
        }
        public String getNombre(){
            return nombre;
        }
        public void setNombre(String nombre){
            this.nombre=nombre;
        }
        public void setCapacidad(int capacidad){
            this.capacidad=capacidad;
        }
        public int getCapacidad(){
            return capacidad;
        }
        public long getVendidas(){
            return vendidas;
        }
        public long getDevueltas(){
            return devueltas;
        }
        public long getEntradasValidas(){
            return Math.max(0,vendidas-devueltas);
        }
        public long getImporteBrutoCentavos(){
            return importeBrutoCentavos;
        }
        public long getImporteReintegradoCentavos(){
            return importeReintegradoCentavos;
        }
        public long getImportePendienteCentavos(){
            return importePendienteCentavos;
        }
        public long getImporteNetoCentavos(){
            return importeBrutoCentavos-importeReintegradoCentavos;
        }
        public void registrarVenta(long importe){
            vendidas++;
            importeBrutoCentavos+=importe;
        }
        public void registrarReintegroAplicado(long importe){
            devueltas++;
            importeReintegradoCentavos+=importe;
        }
        public void registrarPendiente(long importe){
            importePendienteCentavos+=importe;
        }
    }
