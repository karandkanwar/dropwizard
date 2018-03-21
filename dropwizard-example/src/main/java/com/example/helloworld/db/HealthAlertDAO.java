package com.example.helloworld.db;

import com.google.common.collect.ImmutableList;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.time.LocalDate;

public class HealthAlertDAO extends AbstractDAO<HealthAlert> {

    public HealthAlert create(final HealthAlert alert) {
        this.persist(alert);
    }

    public ImmutableList<HealthAlert> findByTypeAndDate(
        final HealthAlertType type,
        final LocalDate alertDate
    ) {
        final Criteria criteria = criteria();
        criteria.add(Restrictions.eq("alertType", type));
        criteria.add(Restrictions.ge("createdAt", alertDate));
        criteria.addOrder(Order.desc("createdAt"));
        return ImmutableList.copyOf(list(criteria));
    }
}
